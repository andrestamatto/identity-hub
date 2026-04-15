package br.dev.andrestamatto.identityhub.domain.model;

import java.util.Set;
import java.util.UUID;

public class User {
    public UUID id;
    public String email;
    public String password;
    public Set<String> roles;

    public User(String email, String password) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.password = password;
        this.roles = Set.of("LIST", "UPDATE", "CREATE");
    }

    public static User getAndre(){
        return new User("andre@test.com", "Password123");
    }
}
