package br.dev.andrestamatto.identityhub.application.usecase;

import br.dev.andrestamatto.identityhub.application.exception.AuthenticationFailedException;
import br.dev.andrestamatto.identityhub.domain.model.Password;
import br.dev.andrestamatto.identityhub.domain.service.AuthProvider;
import br.dev.andrestamatto.identityhub.infrastructure.security.TokenService;
import br.dev.andrestamatto.identityhub.interfaces.rest.dto.LoginResponse;

import java.util.Optional;

public class Login implements Authenticatable {

    private final AuthProvider loginProvider;
    private final TokenService tokenService;

    public Login(AuthProvider authProvider, TokenService tokenService) {
        this.loginProvider = authProvider;
        this.tokenService = tokenService;
    }

    @Override
    public LoginResponse execute(String requestEmail, Password requestPassword) {
        return Optional.ofNullable(loginProvider.authenticate(requestEmail, requestPassword))
                .map(user -> {
                    var token = tokenService.issue(user);
                    return new LoginResponse(token, tokenService.accessTokenExpiresInSeconds());
                })
                .orElseThrow(AuthenticationFailedException::new);
    }
}
