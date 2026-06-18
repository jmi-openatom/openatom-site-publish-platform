package cn.jmi.openatom.sitepublish.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.oauth")
public record OidcProperties(
        String issuer,
        String clientId,
        String clientSecret,
        String scope,
        String redirectUri,
        boolean devLoginEnabled
) {
}

