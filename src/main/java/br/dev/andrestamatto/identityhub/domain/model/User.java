package br.dev.andrestamatto.identityhub.domain.model;

import lombok.Getter;

import java.util.Set;
import java.util.UUID;

@Getter
public class User {
    private UUID id;
    private String email;
    private Password password;
    private Set<String> roles;
    private Set<String> permissions ;

    public User(UUID id, String email, Password password, Set<String> roles, Set<String> permissions) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.roles = roles;
        this.permissions = permissions;
    }

    public User(String email, Password password) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.password = password;
        this.roles = Set.of("USER");
        this.permissions = Set.of();
    }


}
