package br.dev.andrestamatto.identityhub.application.usecase;

import br.dev.andrestamatto.identityhub.application.exception.AuthenticationFailedException;
import br.dev.andrestamatto.identityhub.application.result.AuthenticationResult;
import br.dev.andrestamatto.identityhub.domain.model.RawPassword;
import br.dev.andrestamatto.identityhub.domain.service.PasswordLoginAuthenticator;
import br.dev.andrestamatto.identityhub.application.ports.TokenServicePort;

import java.util.Optional;

public class PasswordLoginUseCase implements PasswordLogin {

    private final String TOKEN_TYPE_BEARER = "Bearer";

    private final PasswordLoginAuthenticator passwordLoginAuthenticator;
    private final TokenServicePort tokenService;

    public PasswordLoginUseCase(PasswordLoginAuthenticator passwordLoginAuthenticator, TokenServicePort tokenService) {
        this.passwordLoginAuthenticator = passwordLoginAuthenticator;
        this.tokenService = tokenService;
    }

    public AuthenticationResult execute(String requestIdentity, RawPassword requestPassword) {
        return Optional.ofNullable(passwordLoginAuthenticator.authenticate(requestIdentity, requestPassword))
                .map(user -> {
                    var token = tokenService.issue(user);
                    return new AuthenticationResult(
                            token,
                            TOKEN_TYPE_BEARER,
                            tokenService.accessTokenExpiresInSeconds()
                    );
                })
                .orElseThrow(AuthenticationFailedException::new);
    }
}
