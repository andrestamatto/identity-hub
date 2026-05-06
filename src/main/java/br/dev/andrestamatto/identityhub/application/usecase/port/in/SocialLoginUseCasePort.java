package br.dev.andrestamatto.identityhub.application.usecase.port.in;

import br.dev.andrestamatto.identityhub.application.result.AuthenticationResult;
import br.dev.andrestamatto.identityhub.application.result.AuthorizationResult;

public interface SocialLoginUseCasePort {
    AuthorizationResult requestAuthorization(String socialProvider);
    AuthenticationResult execute(String socialProvider, String authorizationCode, String redirectUri);
}
