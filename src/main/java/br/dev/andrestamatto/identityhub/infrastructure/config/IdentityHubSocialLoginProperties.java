package br.dev.andrestamatto.identityhub.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@ConfigurationProperties(prefix = "identity-hub.social-login")
public record IdentityHubSocialLoginProperties(
        boolean enabled,
        Map<String, ProviderProperties> providers
) {
    public record ProviderProperties(
            boolean enabled,
            Credentials credentials,
            String defaultRedirectUrl,
            Set<String> allowedRedirectUrls
    ) {}

    public record Credentials(
            String clientId,
            String clientSecret,
            String tokenUrl,
            String userInfoUrl,
            Set<String> scopes
    ){}

    public ProviderProperties getProviderProperties(String provider) {
        var providersList = Optional.ofNullable(this.providers)
                .orElseThrow(() -> new IllegalArgumentException("No social providers configured."));

        return providersList.entrySet().stream()
                        .filter(entry -> entry.getKey() != null && entry.getKey().equalsIgnoreCase(provider))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Provider is not configured: " + provider));
    }
}
