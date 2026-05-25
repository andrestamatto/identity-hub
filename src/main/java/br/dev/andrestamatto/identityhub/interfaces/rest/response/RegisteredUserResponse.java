package br.dev.andrestamatto.identityhub.interfaces.rest.response;

import java.util.Set;

public record RegisteredUserResponse(
     String uuid,
     String username,
     String status,
     Set<String> roles,
     Set<String> permissions,
     String createdAt,
     String verificationMethod,
     String verificationExpiresAt
) {

    public RegisteredUserResponse {
        if (uuid == null || uuid.isBlank()) { throw new IllegalArgumentException("RegisteredUserResponse uuid cannot be null or empty"); }
        if (username == null || username.isBlank()) { throw new IllegalArgumentException("RegisteredUserResponse username cannot be null or empty"); }
        if (status == null || status.isBlank()) { throw new IllegalArgumentException("RegisteredUserResponse status cannot be null or empty"); }
        if (roles == null) { throw new IllegalArgumentException("RegisteredUserResponse role cannot be null or empty"); }
        if (permissions == null) { throw new IllegalArgumentException("RegisteredUserResponse permission cannot be null or empty"); }
        if (createdAt == null || createdAt.isBlank()) { throw new IllegalArgumentException("RegisteredUserResponse createdAt cannot be null or empty"); }
        if (verificationMethod == null || verificationMethod.isBlank()) { throw new IllegalArgumentException("RegisteredUserResponse verificationMethod cannot be null or empty"); }
        if (verificationExpiresAt == null || verificationExpiresAt.isBlank()) {throw new IllegalArgumentException("RegisteredUserResponse verificationExpiresAt cannot be null or empty"); }
    }
    
}
