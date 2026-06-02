package br.dev.andrestamatto.identityhub.domain.valueobjects;

import java.time.Instant;

public record RolePermission(
        Role role,
        Permission permission,
        Instant grantedAt
) {
}
