package br.dev.andrestamatto.identityhub.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "identity-hub.social-login")
public record IdentityHubSocialLoginProperties(
        boolean enabled,
        Map<String, ProviderProperties> providers
) {
    public record ProviderProperties(
            boolean enabled,
            String defaultRedirectUrl,
            List<String> allowedRedirectUrls
    ) {
    }
}
