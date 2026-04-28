package br.dev.andrestamatto.identityhub.infrastructure.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "identity-hub.security")
public record IdentityHubSecurityProperties(
        List<Rule> rules
) {
    public record Rule(
            String pattern,
            String access
    ) {
    }
}
