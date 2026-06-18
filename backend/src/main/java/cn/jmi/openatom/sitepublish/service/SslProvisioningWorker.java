package cn.jmi.openatom.sitepublish.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SslProvisioningWorker {

    private final SslProvisioningService provisioningService;

    @Async
    public void provision(Long domainId) {
        provisioningService.provision(domainId);
    }

    @Async
    public void provisionForSite(Long siteId) {
        provisioningService.provisionForSite(siteId);
    }

    @Scheduled(
            initialDelayString = "${app.ssl.initial-retry-delay-ms:20000}",
            fixedDelayString = "${app.ssl.retry-delay-ms:300000}"
    )
    public void reconcile() {
        provisioningService.reconcile();
    }
}
