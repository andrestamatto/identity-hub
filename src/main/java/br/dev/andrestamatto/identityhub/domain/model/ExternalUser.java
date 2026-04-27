package br.dev.andrestamatto.identityhub.domain.model;

import java.util.Set;
import java.util.UUID;

public record ExternalUser(
        UUID userId,
        String email,
        String encodedPassword,
        Set<String> roles,
        Set<String> permissions
) {
}
