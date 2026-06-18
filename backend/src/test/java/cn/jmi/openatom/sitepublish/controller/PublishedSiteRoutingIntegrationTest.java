package cn.jmi.openatom.sitepublish.controller;

import cn.jmi.openatom.sitepublish.entity.Deployment;
import cn.jmi.openatom.sitepublish.entity.Site;
import cn.jmi.openatom.sitepublish.mapper.DeploymentMapper;
import cn.jmi.openatom.sitepublish.mapper.SiteMapper;
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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SiteMapper siteMapper;

    @Autowired
    private DeploymentMapper deploymentMapper;

    @TempDir
    Path outputDirectory;

    @BeforeEach
    void setUpPublishedSite() throws Exception {
        Files.writeString(outputDirectory.resolve("index.html"), """
                <!doctype html>
                <html lang="zh-CN"><body>host-root-routing-ok</body></html>
                """);

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
}
