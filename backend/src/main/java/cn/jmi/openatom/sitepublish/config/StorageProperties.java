package cn.jmi.openatom.sitepublish.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(Path root, boolean runFrontendBuilds, int buildTimeoutSeconds) {
}

