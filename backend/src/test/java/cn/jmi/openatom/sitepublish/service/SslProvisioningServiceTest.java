package cn.jmi.openatom.sitepublish.service;

import cn.jmi.openatom.sitepublish.config.SslProperties;
import cn.jmi.openatom.sitepublish.entity.Site;
import cn.jmi.openatom.sitepublish.entity.SiteDomain;
import cn.jmi.openatom.sitepublish.mapper.SiteDomainMapper;
import cn.jmi.openatom.sitepublish.mapper.SiteMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SslProvisioningServiceTest {

    @Test
    void marksCertificateActiveAfterSuccessfulCaddyHandshake() throws Exception {
        SiteDomainMapper domainMapper = mock(SiteDomainMapper.class);
        SiteMapper siteMapper = mock(SiteMapper.class);
        TlsCertificateClient certificateClient = mock(TlsCertificateClient.class);
        SslProperties properties = new SslProperties(true, "caddy", 443, 5, 90);
        SslProvisioningService service = new SslProvisioningService(
                domainMapper,
                siteMapper,
                properties,
                certificateClient
        );

        SiteDomain binding = activeBinding();
        Site site = onlineSite();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(90);
        when(domainMapper.selectById(binding.getId())).thenReturn(binding);
        when(siteMapper.selectById(site.getId())).thenReturn(site);
        when(certificateClient.request(binding.getDomain(), properties))
                .thenReturn(new TlsCertificateClient.CertificateResult(expiresAt, "Test CA"));

        service.provision(binding.getId());

        SslProvisioningService.SslState state = service.status(binding, site);
        assertThat(state.status()).isEqualTo("SSL_ACTIVE");
        assertThat(state.expiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void waitsForPublishedSiteBeforeRequestingCertificate() {
        SiteDomainMapper domainMapper = mock(SiteDomainMapper.class);
        SiteMapper siteMapper = mock(SiteMapper.class);
        TlsCertificateClient certificateClient = mock(TlsCertificateClient.class);
        SslProperties properties = new SslProperties(true, "caddy", 443, 5, 90);
        SslProvisioningService service = new SslProvisioningService(
                domainMapper,
                siteMapper,
                properties,
                certificateClient
        );

        SiteDomain binding = activeBinding();
        Site site = onlineSite();
        site.setStatus("PENDING");
        site.setLatestDeploymentId(null);
        when(domainMapper.selectById(binding.getId())).thenReturn(binding);
        when(siteMapper.selectById(site.getId())).thenReturn(site);

        service.provision(binding.getId());

        assertThat(service.status(binding, site).status()).isEqualTo("SSL_WAITING_SITE");
    }

    private SiteDomain activeBinding() {
        SiteDomain binding = new SiteDomain();
        binding.setId(8L);
        binding.setSiteId(5L);
        binding.setDomain("www.example.com");
        binding.setStatus("ACTIVE");
        return binding;
    }

    private Site onlineSite() {
        Site site = new Site();
        site.setId(5L);
        site.setStatus("ONLINE");
        site.setLatestDeploymentId(12L);
        return site;
    }
}
