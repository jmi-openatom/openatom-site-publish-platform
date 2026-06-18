package cn.jmi.openatom.sitepublish.service;

import cn.jmi.openatom.sitepublish.config.StorageProperties;
import cn.jmi.openatom.sitepublish.entity.Deployment;
import cn.jmi.openatom.sitepublish.entity.Site;
import cn.jmi.openatom.sitepublish.mapper.DeploymentMapper;
import cn.jmi.openatom.sitepublish.mapper.SiteMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

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
