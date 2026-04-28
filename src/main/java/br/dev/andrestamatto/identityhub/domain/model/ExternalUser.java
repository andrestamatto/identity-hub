package br.dev.andrestamatto.identityhub.domain.model;

import java.util.Set;
import java.util.UUID;

public record ExternalUser(
        UUID userId,
        String identity,
        EncodedPassword encodedPassword,
        Set<RoleName> roles,
        Set<PermissionName> permissions
) {
}
