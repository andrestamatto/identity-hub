package br.dev.andrestamatto.identityhub.bootstrap.config;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("identityhub.keycloak.management")
public record KeycloakManagementProperties(
        boolean enabled,
        URI baseUri,
        String realm,
        String clientId,
        String clientSecret,
        Duration pollInterval,
        Duration leaseDuration,
        Duration initialRetryDelay,
        int maxAttempts) {

    public KeycloakManagementProperties {
        if (enabled) {
            Objects.requireNonNull(baseUri, "identityhub.keycloak.management.base-uri is required");
            requireText(realm, "identityhub.keycloak.management.realm");
            requireText(clientId, "identityhub.keycloak.management.client-id");
            requireText(clientSecret, "identityhub.keycloak.management.client-secret");
            requirePositive(pollInterval, "identityhub.keycloak.management.poll-interval");
            requirePositive(leaseDuration, "identityhub.keycloak.management.lease-duration");
            requirePositive(
                    initialRetryDelay,
                    "identityhub.keycloak.management.initial-retry-delay");
            if (maxAttempts < 1) {
                throw new IllegalArgumentException(
                        "identityhub.keycloak.management.max-attempts must be positive");
            }
        }
    }

    @Override
    public String toString() {
        return "KeycloakManagementProperties[enabled=" + enabled + ", credentials=REDACTED]";
    }

    private static void requireText(String value, String name) {
        Objects.requireNonNull(value, name + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
    }

    private static void requirePositive(Duration value, String name) {
        Objects.requireNonNull(value, name + " is required");
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
