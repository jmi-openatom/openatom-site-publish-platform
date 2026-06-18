package cn.jmi.openatom.sitepublish.service;

import cn.jmi.openatom.sitepublish.config.DomainProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainRoutingTest {

    @Test
    void normalizesDnsNamesAndTrailingDots() {
        assertThat(DomainService.normalizeDnsName("Site.Example.COM."))
                .isEqualTo("site.example.com");
        assertThat(PublishedSiteService.normalizeHost("WWW.Example.COM:443"))
                .isEqualTo("www.example.com");
    }

    @Test
    void extractsSlugOnlyFromConfiguredPlatformDomain() {
        assertThat(PublishedSiteService.slugFromPlatformHost(
                "site-pfpph.sites.jmi-openatom.cn",
                "sites.jmi-openatom.cn"
        )).isEqualTo("site-pfpph");
        assertThat(PublishedSiteService.slugFromPlatformHost(
                "site-pfpph.attacker.example",
                "sites.jmi-openatom.cn"
        )).isNull();
        assertThat(PublishedSiteService.slugFromPlatformHost(
                "bad.subdomain.sites.jmi-openatom.cn",
                "sites.jmi-openatom.cn"
        )).isNull();
    }

    @Test
    void buildsNormalizedCnameTargetForSiteDetails() {
        DomainProperties properties = new DomainProperties(
                "sites.localhost",
                "Sites.JMI-OpenAtom.CN."
        );

        assertThat(properties.cnameTarget("site-pfpph"))
                .isEqualTo("site-pfpph.sites.jmi-openatom.cn");
    }
}
