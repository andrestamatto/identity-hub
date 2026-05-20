package br.dev.andrestamatto.identityhub.interfaces.rest.response;

import java.util.Set;

public record RegisteredUser(
     String uuid,
     String username,
     String status,
     Set<String> roles,
     Set<String> permissions,
     String createdAt
) {
    
}
