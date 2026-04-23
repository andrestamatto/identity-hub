package br.dev.andrestamatto.identityhub.application.usecase;

import br.dev.andrestamatto.identityhub.domain.service.AuthProvider;
import br.dev.andrestamatto.identityhub.infrastructure.security.TokenIssuer;
import br.dev.andrestamatto.identityhub.interfaces.rest.dto.LoginResponse;


public class Login implements Authenticatable {

    private final AuthProvider authProvider;
    private final TokenIssuer jwtIssuer;

    public Login(AuthProvider authProvider, TokenIssuer jwtIssuer) {
        this.authProvider = authProvider;
        this.jwtIssuer = jwtIssuer;
    }


    @Override
    public LoginResponse execute(String requestEmail, String requestPassword) {
        var user = authProvider.authenticate(requestEmail, requestPassword);
        var token = jwtIssuer.issue(user);
        System.out.println("TOKEN GERADO: " + token);
        return new LoginResponse(token, 86400000);
    }

}
