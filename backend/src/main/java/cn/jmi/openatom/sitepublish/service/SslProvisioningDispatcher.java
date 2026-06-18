package cn.jmi.openatom.sitepublish.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class SslProvisioningDispatcher {

    private final SslProvisioningService provisioningService;
    private final SslProvisioningWorker worker;

    public void dispatchAfterCommit(Long domainId) {
        Runnable dispatch = () -> {
            provisioningService.markProvisioning(domainId);
            worker.provision(domainId);
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            dispatch.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                dispatch.run();
            }
        });
    }

    public void dispatchForSite(Long siteId) {
        worker.provisionForSite(siteId);
    }
}
