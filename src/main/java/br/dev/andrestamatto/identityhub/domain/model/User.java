package br.dev.andrestamatto.identityhub.domain.model;

import lombok.Getter;

import java.util.Set;
import java.util.UUID;

@Getter
public class User {
    private UUID id;
    private String email;
    private String password;
    private Set<String> roles;

    public User(String email, String password) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.password = password;
        this.roles = Set.of("LIST", "UPDATE", "CREATE");
    }

    public User(UUID id, Set<String> roles) {
        this.id = id;
        this.roles = roles;
    }

}
