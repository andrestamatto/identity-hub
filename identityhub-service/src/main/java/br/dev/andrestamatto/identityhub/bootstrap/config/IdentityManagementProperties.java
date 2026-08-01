package br.dev.andrestamatto.identityhub.bootstrap.config;

import java.net.URI;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("identityhub.keycloak.identity-management")
public record IdentityManagementProperties(
        boolean enabled,
        URI baseUri,
        URI publicBaseUri,
        String realm,
        String clientId,
        String clientSecret) {

    public IdentityManagementProperties {
        if (enabled) {
            Objects.requireNonNull(
                    baseUri,
                    "identityhub.keycloak.identity-management.base-uri is required");
            Objects.requireNonNull(
                    publicBaseUri,
                    "identityhub.keycloak.identity-management.public-base-uri is required");
            requireText(realm, "identityhub.keycloak.identity-management.realm");
            requireText(clientId, "identityhub.keycloak.identity-management.client-id");
            requireText(
                    clientSecret,
                    "identityhub.keycloak.identity-management.client-secret");
        }
    }

    @Override
    public String toString() {
        return "IdentityManagementProperties[enabled=" + enabled
                + ", credentials=REDACTED]";
    }

    private static void requireText(String value, String name) {
        Objects.requireNonNull(value, name + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
    }
}
