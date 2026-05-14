package br.dev.andrestamatto.identityhub.domain.valueobjects;

import br.dev.andrestamatto.identityhub.domain.entities.User;

import java.time.Instant;

public record UserPermission(
        User user,
        Permission permission,
        boolean isAllowed,
        Instant expiresAt,
        User grantedBy,
        Instant grantedAt
) {
}
