package br.dev.andrestamatto.identityhub.domain.valueobjects;

import java.util.UUID;

public record Permission(
        UUID uuid,
        String name,
        String slug,
        String description
) {
}
