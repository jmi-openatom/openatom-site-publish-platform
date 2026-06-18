package cn.jmi.openatom.sitepublish.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class SiteBuildDispatcher {

    private final SiteBuildWorker buildWorker;

    public void dispatchAfterCommit(Long deploymentId, Long siteId, Path sourcePath) {
        Path absoluteSourcePath = sourcePath.toAbsolutePath();
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            buildWorker.build(deploymentId, siteId, absoluteSourcePath);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                buildWorker.build(deploymentId, siteId, absoluteSourcePath);
            }
        });
    }
}
