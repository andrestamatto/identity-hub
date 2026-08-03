package br.dev.andrestamatto.identityhub.access.adapter.out.keycloak;

import java.util.Map;
import java.util.Objects;

public record KeycloakClientRole(String clientInternalId, String roleId, String roleName) {

    public KeycloakClientRole {
        Objects.requireNonNull(clientInternalId);
        Objects.requireNonNull(roleId);
        Objects.requireNonNull(roleName);
    }

    Map<String, Object> representation() {
        return Map.of(
                "id", roleId,
                "name", roleName,
                "clientRole", true,
                "containerId", clientInternalId);
    }
}
