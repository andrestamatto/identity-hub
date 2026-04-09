package br.dev.andrestamatto.identityhub.domain.model;

import java.util.Set;
import java.util.UUID;

public class User {
    public UUID id;
    public String email;
    public String password;
    public Set<String> roles;
}
