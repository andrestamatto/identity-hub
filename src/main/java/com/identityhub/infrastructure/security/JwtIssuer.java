package com.identityhub.infrastructure.security;

import com.identityhub.domain.model.User;

public class JwtIssuer implements TokenIssuer {

    private final String SECRET = "my-secret-key";

    @Override
    public String issue(User user) {
        return "";
    }
}
