package br.dev.andrestamatto.identityhub.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="identity-hub.security")
public record SecurityProperties(
    String apiSecret
) {
    public SecurityProperties {
        if (apiSecret == null || apiSecret.isBlank()) throw new IllegalArgumentException("apiSecret cannot be null or blank");
    }
}
