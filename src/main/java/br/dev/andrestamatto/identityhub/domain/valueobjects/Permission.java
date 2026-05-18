package br.dev.andrestamatto.identityhub.domain.valueobjects;

import java.time.Instant;
import java.util.UUID;

/**
 * Permission representa uma capacidade específica que pode ser concedida ao usuário.
 * Exemplo: user:read, user:write, billing:refund.
 * Permissões são granulares e podem ser associadas a roles ou diretamente ao usuário.
 */
public record Permission(
        UUID uuid,
        String name,
        String slug,
        String description,
        Instant createdAt
) {
}
