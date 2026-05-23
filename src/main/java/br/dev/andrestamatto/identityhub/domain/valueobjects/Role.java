package br.dev.andrestamatto.identityhub.domain.valueobjects;

import java.time.Instant;
import java.util.UUID;

/**
 * Role representa um papel de acesso de alto nível atribuído ao usuário.
 * Exemplo: ADMIN, SUPPORT, CUSTOMER.
 * Uma role agrupa permissões para facilitar autorização por responsabilidade.
 */
public record Role(
        UUID roleId,
        String roleName,
        String roleDescription,
        Instant createdAt
) {

    public Role {
        if (roleId == null) { throw new IllegalArgumentException("roleId cannot be null"); }
        if (roleName == null) { throw new IllegalArgumentException("roleName cannot be null"); }
        if (roleDescription == null) { throw new IllegalArgumentException("roleDescription cannot be null"); }
        if (createdAt == null ) { throw new IllegalArgumentException("createdAt cannot be null or empty"); }
    }

}
