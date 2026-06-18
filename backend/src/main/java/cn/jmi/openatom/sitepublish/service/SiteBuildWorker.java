package cn.jmi.openatom.sitepublish.service;

import cn.jmi.openatom.sitepublish.config.StorageProperties;
import cn.jmi.openatom.sitepublish.entity.Deployment;
import cn.jmi.openatom.sitepublish.entity.Site;
import cn.jmi.openatom.sitepublish.mapper.DeploymentMapper;
import cn.jmi.openatom.sitepublish.mapper.SiteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiteBuildWorker {

    private final DeploymentMapper deploymentMapper;
    private final SiteMapper siteMapper;
    private final StorageProperties properties;

    @Async
    public void build(Long deploymentId, Long siteId, Path sourcePath) {
        LocalDateTime started = LocalDateTime.now();
        Deployment deployment = deploymentMapper.selectById(deploymentId);
        Site site = siteMapper.selectById(siteId);
        StringBuilder buildLog = new StringBuilder();
        Path workDirectory = properties.root().resolve("work").resolve(String.valueOf(deploymentId));
        Path outputDirectory = properties.root().resolve("published")
                .resolve(site.getSlug())
                .resolve(String.valueOf(deploymentId));
        try {
            deleteDirectory(workDirectory);
            Files.createDirectories(workDirectory);
            Files.createDirectories(outputDirectory);
            buildLog.append("✓ 创建隔离构建目录\n");

            Path sourceDirectory = prepareSource(sourcePath, workDirectory, buildLog);
            Path publishDirectory = findPublishDirectory(sourceDirectory, buildLog);
            copyDirectory(publishDirectory, outputDirectory);
            buildLog.append("✓ 复制发布产物\n");
            buildLog.append("✓ 发布完成：/published/").append(site.getSlug()).append("/\n");

            deployment.setStatus("SUCCESS");
            deployment.setOutputPath(outputDirectory.toAbsolutePath().toString());
            site.setStatus("ONLINE");
        } catch (Exception exception) {
            log.warn("Deployment {} failed", deploymentId, exception);
            deployment.setStatus("FAILED");
            site.setStatus("FAILED");
            buildLog.append("✕ 构建失败：").append(exception.getMessage()).append('\n');
        }

        LocalDateTime finished = LocalDateTime.now();
        deployment.setBuildLog(buildLog.toString());
        deployment.setFinishedAt(finished);
        deployment.setDurationSeconds((int) Duration.between(started, finished).toSeconds());
        deploymentMapper.updateById(deployment);

        site.setLatestDeploymentId(deployment.getId());
        site.setUpdatedAt(finished);
        siteMapper.updateById(site);
    }

    private Path prepareSource(Path sourcePath, Path workDirectory, StringBuilder buildLog) throws IOException {
        if (Files.isDirectory(sourcePath)) {
            copyDirectory(sourcePath, workDirectory);
            buildLog.append("✓ 读取模板源文件\n");
            return workDirectory;
        }
        String name = sourcePath.getFileName().toString().toLowerCase();
        if (name.endsWith(".html") || name.endsWith(".htm")) {
            Files.copy(sourcePath, workDirectory.resolve("index.html"), StandardCopyOption.REPLACE_EXISTING);
            buildLog.append("✓ 识别为静态 HTML 项目\n");
            return workDirectory;
        }
        unzipSecurely(sourcePath, workDirectory);
        buildLog.append("✓ 解压项目文件\n");
        return collapseSingleDirectory(workDirectory);
    }

    private Path findPublishDirectory(Path sourceDirectory, StringBuilder buildLog) throws Exception {
        Path dist = sourceDirectory.resolve("dist");
        Path build = sourceDirectory.resolve("build");
        if (Files.isRegularFile(sourceDirectory.resolve("index.html"))) {
            buildLog.append("✓ 检测到静态站点入口\n");
            return sourceDirectory;
        }
        if (Files.isRegularFile(dist.resolve("index.html"))) {
            buildLog.append("✓ 检测到 dist 构建产物\n");
            return dist;
        }
        if (Files.isRegularFile(build.resolve("index.html"))) {
            buildLog.append("✓ 检测到 build 构建产物\n");
            return build;
        }
        if (Files.isRegularFile(sourceDirectory.resolve("package.json"))) {
            if (!properties.runFrontendBuilds()) {
                throw new IllegalStateException("检测到源码项目，但服务端自动执行 npm 构建已关闭");
            }
            runCommand(sourceDirectory, buildLog, "npm", "install", "--no-audit", "--no-fund");
            runCommand(sourceDirectory, buildLog, "npm", "run", "build");
            if (Files.isRegularFile(dist.resolve("index.html"))) {
                return dist;
            }
            if (Files.isRegularFile(build.resolve("index.html"))) {
                return build;
            }
            throw new IllegalStateException("构建成功但未找到 dist 或 build/index.html");
        }
        throw new IllegalStateException("项目中未找到 index.html 或 package.json");
    }

    private void runCommand(Path directory, StringBuilder buildLog, String... command) throws Exception {
        buildLog.append("$ ").append(String.join(" ", command)).append('\n');
        Process process = new ProcessBuilder(command)
                .directory(directory.toFile())
                .redirectErrorStream(true)
                .start();
        StringBuilder output = new StringBuilder();
        Thread reader = Thread.startVirtualThread(() -> {
            try (BufferedReader buffered = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = buffered.readLine()) != null && output.length() < 20_000) {
                    output.append(line).append('\n');
                }
            } catch (IOException ignored) {
            }
        });
        boolean finished = process.waitFor(properties.buildTimeoutSeconds(), TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("前端构建超时");
        }
        reader.join(Duration.ofSeconds(2));
        buildLog.append(output);
        if (process.exitValue() != 0) {
            throw new IllegalStateException("命令执行失败：" + String.join(" ", command));
        }
    }

    private void unzipSecurely(Path zipPath, Path target) throws IOException {
        try (ZipInputStream input = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                Path destination = target.resolve(entry.getName()).normalize();
                if (!destination.startsWith(target)) {
                    throw new IOException("ZIP 包含不安全路径");
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private Path collapseSingleDirectory(Path directory) throws IOException {
        try (var children = Files.list(directory)) {
            var entries = children.toList();
            if (entries.size() == 1 && Files.isDirectory(entries.getFirst())) {
                return entries.getFirst();
            }
        }
        return directory;
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        try (var paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path destination = target.resolve(source.relativize(path));
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}

