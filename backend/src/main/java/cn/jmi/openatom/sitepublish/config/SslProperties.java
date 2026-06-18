package cn.jmi.openatom.sitepublish.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ssl")
public record SslProperties(
        boolean enabled,
        String proxyHost,
        int proxyPort,
        int connectTimeoutSeconds,
        int handshakeTimeoutSeconds
) {
}
