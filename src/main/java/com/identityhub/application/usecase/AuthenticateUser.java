package com.identityhub.application.usecase;

import com.identityhub.domain.service.AuthProvider;
import com.identityhub.infrastructure.security.TokenIssuer;

public class AuthenticateUser implements Authenticate {

    private final AuthProvider authProvider;
    private final TokenIssuer jwtIssuer;

    public AuthenticateUser(AuthProvider authProvider, TokenIssuer jwtIssuer) {
        this.authProvider = authProvider;
        this.jwtIssuer = jwtIssuer;
    }


    @Override
    public String execute(String email, String password) {
        var user = authProvider.authenticate(email, password);
        return jwtIssuer.issue(user);
    }

}
