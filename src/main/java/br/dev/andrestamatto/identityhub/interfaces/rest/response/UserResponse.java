package br.dev.andrestamatto.identityhub.interfaces.rest.response;

import java.util.Set;

public record UserResponse(
     String uuid,
     String username,
     String status,
     Set<String> roles,
     Set<String> permissions,
     String createdAt,
     String verificationMethod,
     String verificationExpiresAt
) {

    public UserResponse {
        if (uuid == null || uuid.isBlank()) { throw new IllegalArgumentException("UserResponse uuid cannot be null or empty"); }
        if (username == null || username.isBlank()) { throw new IllegalArgumentException("UserResponse username cannot be null or empty"); }
        if (status == null || status.isBlank()) { throw new IllegalArgumentException("UserResponse status cannot be null or empty"); }
        if (roles == null) { throw new IllegalArgumentException("UserResponse role cannot be null or empty"); }
        if (permissions == null) { throw new IllegalArgumentException("UserResponse permission cannot be null or empty"); }
        if (createdAt == null || createdAt.isBlank()) { throw new IllegalArgumentException("UserResponse createdAt cannot be null or empty"); }
    }
    
}
