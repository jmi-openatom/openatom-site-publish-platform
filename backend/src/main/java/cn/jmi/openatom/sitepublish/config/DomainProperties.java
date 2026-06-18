package cn.jmi.openatom.sitepublish.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Locale;

@ConfigurationProperties(prefix = "app.domains")
public record DomainProperties(
        String publicBaseDomain,
        String cnameBaseDomain
) {

    public String cnameTarget(String slug) {
        String baseDomain = normalize(cnameBaseDomain);
        return slug == null || slug.isBlank() ? baseDomain : slug + "." + baseDomain;
    }

    private static String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return normalized.endsWith(".") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }
}
