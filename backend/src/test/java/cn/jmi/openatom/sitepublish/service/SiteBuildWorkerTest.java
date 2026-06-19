package cn.jmi.openatom.sitepublish.service;

import cn.jmi.openatom.sitepublish.config.StorageProperties;
import cn.jmi.openatom.sitepublish.entity.Deployment;
import cn.jmi.openatom.sitepublish.entity.Site;
import cn.jmi.openatom.sitepublish.mapper.DeploymentMapper;
import cn.jmi.openatom.sitepublish.mapper.SiteMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SiteBuildWorkerTest {

    private final DeploymentMapper deploymentMapper = mock(DeploymentMapper.class);
    private final SiteMapper siteMapper = mock(SiteMapper.class);

    @TempDir
    Path storageRoot;

    @Test
    void exitsSafelyWhenDeploymentWasDeleted() {
        SiteBuildWorker worker = createWorker();
        when(deploymentMapper.selectById(101L)).thenReturn(null);

        worker.build(101L, 202L, storageRoot.resolve("source"));

        verify(deploymentMapper, never()).updateById(any(Deployment.class));
        verify(siteMapper, never()).updateById(any(Site.class));
    }

    @Test
    void exitsSafelyWhenSiteWasDeleted() {
        SiteBuildWorker worker = createWorker();
        when(deploymentMapper.selectById(303L)).thenReturn(new Deployment());
        when(siteMapper.selectById(404L)).thenReturn(null);

        worker.build(303L, 404L, storageRoot.resolve("source"));

        verify(deploymentMapper, never()).updateById(any(Deployment.class));
        verify(siteMapper, never()).updateById(any(Site.class));
    }

    @Test
    void publishesZipCreatedByMacOsWithMetadataDirectory() throws IOException {
        SiteBuildWorker worker = createWorker();
        Deployment deployment = new Deployment();
        deployment.setId(505L);
        Site site = new Site();
        site.setId(606L);
        site.setSlug("macos-zip");
        when(deploymentMapper.selectById(505L)).thenReturn(deployment);
        when(siteMapper.selectById(606L)).thenReturn(site);

        Path zipPath = storageRoot.resolve("macos-project.zip");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            addZipEntry(output, "demo/index.html", "<h1>发布成功</h1>");
            addZipEntry(output, "demo/assets/app.js", "console.log('ok')");
            addZipEntry(output, "demo/.DS_Store", "metadata");
            addZipEntry(output, "__MACOSX/._demo", "metadata");
            addZipEntry(output, "__MACOSX/demo/._index.html", "metadata");
        }

        worker.build(505L, 606L, zipPath);

        ArgumentCaptor<Deployment> deploymentCaptor = ArgumentCaptor.forClass(Deployment.class);
        verify(deploymentMapper).updateById(deploymentCaptor.capture());
        Deployment savedDeployment = deploymentCaptor.getValue();
        assertEquals("SUCCESS", savedDeployment.getStatus());
        assertTrue(savedDeployment.getBuildLog().contains("✓ 解压项目文件"));

        Path outputDirectory = Path.of(savedDeployment.getOutputPath());
        assertTrue(Files.isRegularFile(outputDirectory.resolve("index.html")));
        assertTrue(Files.isRegularFile(outputDirectory.resolve("assets/app.js")));
        assertFalse(Files.exists(outputDirectory.resolve("__MACOSX")));
        assertFalse(Files.exists(outputDirectory.resolve(".DS_Store")));
    }

    private void addZipEntry(ZipOutputStream output, String name, String content) throws IOException {
        output.putNextEntry(new ZipEntry(name));
        output.write(content.getBytes(StandardCharsets.UTF_8));
        output.closeEntry();
    }

    private SiteBuildWorker createWorker() {
        StorageProperties properties = new StorageProperties(storageRoot, false, 30);
        return new SiteBuildWorker(
                deploymentMapper,
                siteMapper,
                properties,
                org.mockito.Mockito.mock(SslProvisioningDispatcher.class)
        );
    }
}
