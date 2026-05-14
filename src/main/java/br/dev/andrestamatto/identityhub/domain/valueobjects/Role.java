package br.dev.andrestamatto.identityhub.domain.valueobjects;

import java.util.Set;
import java.util.UUID;

public record Role(
        UUID roleId,
        String roleName,
        String roleDescription,
        Set<RolePermission> rolePermissions
) {

    public Role {
        if (roleId == null) { throw new IllegalArgumentException("roleId cannot be null"); }
        if (roleName == null) { throw new IllegalArgumentException("roleName cannot be null"); }
        if (roleDescription == null) { throw new IllegalArgumentException("roleDescription cannot be null"); }
        if (rolePermissions == null ||  rolePermissions.isEmpty() ) { throw new IllegalArgumentException("rolePermissions cannot be null or empty"); }
    }

}
