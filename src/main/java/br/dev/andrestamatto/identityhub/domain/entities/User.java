package br.dev.andrestamatto.identityhub.domain.entities;

import br.dev.andrestamatto.identityhub.domain.valueobjects.*;

import java.security.Permission;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record User(
       UUID uuid,
       Username username,
       EncodedPassword password,
       UserStatus status,
       Set<Role> roles,
       Set<Permission> permissions,
       List<LoginAttempt> failedLoginAttempts,
       Integer failedLoginCount,
       Instant lasFailedLoginAt,
       Instant lockedUntil,
       Instant createdAt,
       Instant passwordChangedAt,
       Instant updatedAt
) {
}
