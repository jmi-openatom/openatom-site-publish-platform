package cn.jmi.openatom.sitepublish.controller;

import cn.jmi.openatom.sitepublish.entity.Deployment;
import cn.jmi.openatom.sitepublish.entity.Site;
import cn.jmi.openatom.sitepublish.entity.SiteDomain;
import cn.jmi.openatom.sitepublish.mapper.DeploymentMapper;
import cn.jmi.openatom.sitepublish.mapper.SiteDomainMapper;
import cn.jmi.openatom.sitepublish.mapper.SiteMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:published-site-routing;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "app.domains.public-base-domain=sites.localhost"
})
@AutoConfigureMockMvc
class PublishedSiteRoutingIntegrationTest {

    private static final long SITE_ID = 10_001L;
    private static final long DEPLOYMENT_ID = 10_001L;
    private static final String SLUG = "routing-test";
    private static final String HOST = SLUG + ".sites.localhost";
    private static final String CUSTOM_HOST = "www.routing-test.example";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SiteMapper siteMapper;

    @Autowired
    private DeploymentMapper deploymentMapper;

    @Autowired
    private SiteDomainMapper siteDomainMapper;

    @TempDir
    Path outputDirectory;

    @BeforeEach
    void setUpPublishedSite() throws Exception {
        Files.writeString(outputDirectory.resolve("index.html"), """
                <!doctype html>
                <html lang="zh-CN"><body>host-root-routing-ok</body></html>
                """);

        siteDomainMapper.delete(Wrappers.<SiteDomain>lambdaQuery().eq(SiteDomain::getSiteId, SITE_ID));
        deploymentMapper.deleteById(DEPLOYMENT_ID);
        siteMapper.deleteById(SITE_ID);

        LocalDateTime now = LocalDateTime.now();
        Deployment deployment = new Deployment();
        deployment.setId(DEPLOYMENT_ID);
        deployment.setSiteId(SITE_ID);
        deployment.setUserId(1L);
        deployment.setStatus("SUCCESS");
        deployment.setEnvironment("production");
        deployment.setOutputPath(outputDirectory.toAbsolutePath().toString());
        deployment.setStartedAt(now);
        deployment.setFinishedAt(now);
        deployment.setCreatedAt(now);
        deploymentMapper.insert(deployment);

        Site site = new Site();
        site.setId(SITE_ID);
        site.setUserId(1L);
        site.setName("Host 路由测试站点");
        site.setSlug(SLUG);
        site.setFramework("HTML");
        site.setStatus("ONLINE");
        site.setDefaultDomain(HOST);
        site.setLatestDeploymentId(DEPLOYMENT_ID);
        site.setCreatedAt(now);
        site.setUpdatedAt(now);
        siteMapper.insert(site);
    }

    @Test
    void servesPublishedSiteFromHostRootBeforeStaticResourceFallback() throws Exception {
        mockMvc.perform(get("/").header("Host", HOST))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("host-root-routing-ok")));
    }

    @Test
    void keepsPathAccessAndCertificateAuthorizationConsistent() throws Exception {
        mockMvc.perform(get("/published/" + SLUG + "/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("host-root-routing-ok")));

        mockMvc.perform(get("/api/public/domains/allow").param("domain", HOST))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/").header("Host", "unknown.sites.localhost"))
                .andExpect(status().isNotFound());
    }

    @Test
    void servesVerifiedCustomDomainWhenProxyPreservesOriginalHost() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        SiteDomain binding = new SiteDomain();
        binding.setSiteId(SITE_ID);
        binding.setUserId(1L);
        binding.setDomain(CUSTOM_HOST);
        binding.setType("CUSTOM");
        binding.setStatus("ACTIVE");
        binding.setVerificationToken("site-verify-routing-test");
        binding.setCreatedAt(now);
        binding.setUpdatedAt(now);
        siteDomainMapper.insert(binding);

        mockMvc.perform(get("/").header("Host", CUSTOM_HOST))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("host-root-routing-ok")));

        mockMvc.perform(get("/api/public/domains/allow").param("domain", CUSTOM_HOST))
                .andExpect(status().isNoContent());
    }
}
