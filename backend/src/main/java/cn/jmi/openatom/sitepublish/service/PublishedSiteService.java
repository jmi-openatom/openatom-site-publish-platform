package cn.jmi.openatom.sitepublish.service;

import cn.jmi.openatom.sitepublish.config.DomainProperties;
import cn.jmi.openatom.sitepublish.entity.Deployment;
import cn.jmi.openatom.sitepublish.entity.Site;
import cn.jmi.openatom.sitepublish.entity.SiteDomain;
import cn.jmi.openatom.sitepublish.mapper.DeploymentMapper;
import cn.jmi.openatom.sitepublish.mapper.SiteDomainMapper;
import cn.jmi.openatom.sitepublish.mapper.SiteMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PublishedSiteService {

    private final SiteMapper siteMapper;
    private final SiteDomainMapper domainMapper;
    private final DeploymentMapper deploymentMapper;
    private final DomainProperties domainProperties;

    public Site findBySlug(String slug) {
        return siteMapper.selectOne(Wrappers.<Site>lambdaQuery().eq(Site::getSlug, slug));
    }

    public Site findByHost(String rawHost) {
        String host = normalizeHost(rawHost);
        if (!StringUtils.hasText(host)) {
            return null;
        }

        SiteDomain binding = domainMapper.selectOne(Wrappers.<SiteDomain>lambdaQuery()
                .eq(SiteDomain::getDomain, host)
                .eq(SiteDomain::getStatus, "ACTIVE"));
        if (binding != null) {
            return siteMapper.selectById(binding.getSiteId());
        }

        Site defaultDomainSite = siteMapper.selectOne(Wrappers.<Site>lambdaQuery()
                .eq(Site::getDefaultDomain, host));
        if (defaultDomainSite != null) {
            return defaultDomainSite;
        }

        String slug = slugFromPlatformHost(host, domainProperties.publicBaseDomain());
        if (slug == null) {
            slug = slugFromPlatformHost(host, domainProperties.cnameBaseDomain());
        }
        return slug == null ? null : findBySlug(slug);
    }

    public boolean canServeHost(String rawHost) {
        Site site = findByHost(rawHost);
        return site != null && site.getLatestDeploymentId() != null && "ONLINE".equals(site.getStatus());
    }

    public ResponseEntity<Resource> serve(Site site, String requestedPath) {
        if (site == null || site.getLatestDeploymentId() == null) {
            return ResponseEntity.notFound().build();
        }
        Deployment deployment = deploymentMapper.selectById(site.getLatestDeploymentId());
        if (deployment == null || !"SUCCESS".equals(deployment.getStatus()) || deployment.getOutputPath() == null) {
            return ResponseEntity.notFound().build();
        }

        String relative = requestedPath == null ? "" : requestedPath.replaceFirst("^/+", "");
        if (relative.isBlank() || relative.endsWith("/")) {
            relative += "index.html";
        }

        Path root = Path.of(deployment.getOutputPath()).normalize();
        Path file = root.resolve(relative).normalize();
        if (!file.startsWith(root)) {
            return ResponseEntity.badRequest().build();
        }
        if (!Files.isRegularFile(file)) {
            Path spaFallback = root.resolve("index.html");
            if (!Files.isRegularFile(spaFallback)) {
                return ResponseEntity.notFound().build();
            }
            file = spaFallback;
        }
        MediaType mediaType = MediaTypeFactory.getMediaType(file.getFileName().toString())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(mediaType).body(new FileSystemResource(file));
    }

    static String normalizeHost(String rawHost) {
        if (!StringUtils.hasText(rawHost)) {
            return null;
        }
        String host = rawHost.trim().toLowerCase(Locale.ROOT);
        if (host.startsWith("[")) {
            int closingBracket = host.indexOf(']');
            return closingBracket > 0 ? host.substring(1, closingBracket) : host;
        }
        int colon = host.indexOf(':');
        if (colon >= 0) {
            host = host.substring(0, colon);
        }
        return trimTrailingDot(host);
    }

    static String slugFromPlatformHost(String host, String baseDomain) {
        String normalizedBase = normalizeHost(baseDomain);
        if (!StringUtils.hasText(host) || !StringUtils.hasText(normalizedBase)) {
            return null;
        }
        String suffix = "." + normalizedBase;
        if (!host.endsWith(suffix)) {
            return null;
        }
        String slug = host.substring(0, host.length() - suffix.length());
        return slug.matches("^[a-z0-9][a-z0-9-]{1,48}[a-z0-9]$") ? slug : null;
    }

    private static String trimTrailingDot(String value) {
        return value.endsWith(".") ? value.substring(0, value.length() - 1) : value;
    }
}
