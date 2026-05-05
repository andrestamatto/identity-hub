package br.dev.andrestamatto.identityhub.application.usecase.port.in;

import br.dev.andrestamatto.identityhub.application.result.AuthenticationResult;

public interface SocialLoginUseCasePort {
    AuthenticationResult execute(String provider, String authorizationCode, String redirectUri);
}
