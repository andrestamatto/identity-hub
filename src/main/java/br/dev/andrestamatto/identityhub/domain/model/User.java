package br.dev.andrestamatto.identityhub.domain.model;

import lombok.Getter;

import java.util.Set;
import java.util.UUID;

@Getter
public class User {
    private UUID id;
    private String identity;
    private EncodedPassword encodedPassword;
    private Set<RoleName> roles;
    private Set<PermissionName> permissions;

    public User(UUID id, String identity, EncodedPassword password, Set<RoleName> roles, Set<PermissionName> permissions) {
        this.id = id;
        this.identity = identity;
        this.encodedPassword = password;
        this.roles = roles;
        this.permissions = permissions;
    }
}
