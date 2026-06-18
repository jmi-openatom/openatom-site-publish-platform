package cn.jmi.openatom.sitepublish.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.jmi.openatom.sitepublish.common.BusinessException;
import cn.jmi.openatom.sitepublish.config.StorageProperties;
import cn.jmi.openatom.sitepublish.dto.SiteDtos;
import cn.jmi.openatom.sitepublish.entity.Deployment;
import cn.jmi.openatom.sitepublish.entity.Site;
import cn.jmi.openatom.sitepublish.mapper.DeploymentMapper;
import cn.jmi.openatom.sitepublish.mapper.SiteMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeploymentService {

    private final DeploymentMapper deploymentMapper;
    private final SiteMapper siteMapper;
    private final SiteService siteService;
    private final StorageProperties storageProperties;
    private final SiteBuildDispatcher buildDispatcher;

    public List<SiteDtos.DeploymentView> recent(Integer limit) {
        Long userId = StpUtil.getLoginIdAsLong();
        int safeLimit = Math.min(Math.max(limit == null ? 20 : limit, 1), 100);
        return deploymentMapper.selectList(Wrappers.<Deployment>lambdaQuery()
                        .eq(Deployment::getUserId, userId)
                        .orderByDesc(Deployment::getCreatedAt)
                        .last("LIMIT " + safeLimit))
                .stream()
                .map(deployment -> siteService.toDeploymentView(deployment, siteMapper.selectById(deployment.getSiteId())))
                .toList();
    }

    public List<SiteDtos.DeploymentView> bySite(Long siteId) {
        Site site = siteService.requireOwned(siteId);
        return deploymentMapper.selectList(Wrappers.<Deployment>lambdaQuery()
                        .eq(Deployment::getSiteId, site.getId())
                        .orderByDesc(Deployment::getCreatedAt))
                .stream()
                .map(deployment -> siteService.toDeploymentView(deployment, site))
                .toList();
    }

    @Transactional
    public SiteDtos.DeploymentView uploadAndDeploy(Long siteId, MultipartFile file) {
        Site site = siteService.requireOwned(siteId);
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择需要发布的 HTML 文件或 ZIP 项目包");
        }
        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        String lower = originalFilename.toLowerCase();
        if (!lower.endsWith(".zip") && !lower.endsWith(".html") && !lower.endsWith(".htm")) {
            throw new BusinessException("目前支持 .zip、.html 和 .htm 文件");
        }
        try {
            Path uploadDirectory = storageProperties.root().resolve("uploads").resolve(site.getSlug());
            Files.createDirectories(uploadDirectory);
            Path sourcePath = uploadDirectory.resolve(System.currentTimeMillis() + "-" + originalFilename);
            Files.copy(file.getInputStream(), sourcePath, StandardCopyOption.REPLACE_EXISTING);
            site.setSourceFilename(originalFilename);
            site.setSourcePath(sourcePath.toAbsolutePath().toString());
            siteMapper.updateById(site);
            return createDeployment(site, sourcePath);
        } catch (IOException exception) {
            throw new BusinessException("项目文件保存失败");
        }
    }

    @Transactional
    public SiteDtos.DeploymentView deployTemplate(Long siteId, String templateId) {
        Site site = siteService.requireOwned(siteId);
        try {
            Path templateDirectory = storageProperties.root().resolve("templates")
                    .resolve(site.getSlug() + "-" + System.currentTimeMillis());
            Files.createDirectories(templateDirectory);
            Files.writeString(templateDirectory.resolve("index.html"), templateHtml(site, templateId));
            site.setSourceFilename("template-" + templateId);
            site.setSourcePath(templateDirectory.toAbsolutePath().toString());
            siteMapper.updateById(site);
            return createDeployment(site, templateDirectory);
        } catch (IOException exception) {
            throw new BusinessException("模板创建失败");
        }
    }

    @Transactional
    public SiteDtos.DeploymentView redeploy(Long siteId) {
        Site site = siteService.requireOwned(siteId);
        if (site.getSourcePath() == null || !Files.exists(Path.of(site.getSourcePath()))) {
            throw new BusinessException("该项目还没有可重新发布的源文件");
        }
        return createDeployment(site, Path.of(site.getSourcePath()));
    }

    private SiteDtos.DeploymentView createDeployment(Site site, Path sourcePath) {
        Deployment deployment = new Deployment();
        deployment.setSiteId(site.getId());
        deployment.setUserId(site.getUserId());
        deployment.setStatus("BUILDING");
        deployment.setEnvironment("production");
        deployment.setCommitHash(randomHash());
        deployment.setSourceFilename(site.getSourceFilename());
        deployment.setBuildLog("已接收项目，正在排队构建……");
        deployment.setStartedAt(LocalDateTime.now());
        deployment.setCreatedAt(LocalDateTime.now());
        deploymentMapper.insert(deployment);

        site.setStatus("BUILDING");
        site.setLatestDeploymentId(deployment.getId());
        site.setUpdatedAt(LocalDateTime.now());
        siteMapper.updateById(site);
        buildDispatcher.dispatchAfterCommit(deployment.getId(), site.getId(), sourcePath);
        return siteService.toDeploymentView(deployment, site);
    }

    private String templateHtml(Site site, String templateId) {
        String accent = "portfolio".equalsIgnoreCase(templateId) ? "#7928ca" : "#0070f3";
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width,initial-scale=1">
                  <title>%s</title>
                  <style>
                    *{box-sizing:border-box}body{margin:0;font-family:Inter,-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif;color:#171717;background:#fafafa}
                    main{min-height:100vh;display:grid;place-items:center;padding:32px}.hero{max-width:880px;padding:72px;border:1px solid #ebebeb;border-radius:16px;background:#fff;box-shadow:0 8px 30px rgba(0,0,0,.05)}
                    .eyebrow{font:13px ui-monospace;color:%s}.title{font-size:56px;line-height:1;letter-spacing:-3px;margin:24px 0}.copy{font-size:18px;line-height:1.7;color:#555;max-width:620px}
                    a{display:inline-block;margin-top:32px;padding:12px 20px;border-radius:999px;color:#fff;background:#171717;text-decoration:none}
                  </style>
                </head>
                <body><main><section class="hero"><div class="eyebrow">SITE PUBLISH / LIVE</div><h1 class="title">%s.</h1><p class="copy">%s</p><a href="#">开始探索</a></section></main></body>
                </html>
                """.formatted(site.getName(), accent, site.getName(),
                site.getDescription() == null || site.getDescription().isBlank()
                        ? "这是由 Site Publish 自动创建并发布的网站。"
                        : site.getDescription());
    }

    private String sanitizeFilename(String filename) {
        String safe = filename == null ? "site.zip" : Path.of(filename).getFileName().toString();
        return safe.replaceAll("[^a-zA-Z0-9._-]", "-");
    }

    private String randomHash() {
        byte[] bytes = new byte[4];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
