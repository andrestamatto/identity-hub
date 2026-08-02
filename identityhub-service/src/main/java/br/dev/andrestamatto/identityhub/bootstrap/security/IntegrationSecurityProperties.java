package br.dev.andrestamatto.identityhub.bootstrap.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("identityhub.security.integration")
public record IntegrationSecurityProperties(String audience) {

    public IntegrationSecurityProperties {
        if (audience == null || audience.isBlank()) {
            throw new IllegalArgumentException(
                    "identityhub.security.integration.audience is required");
        }
    }
}
