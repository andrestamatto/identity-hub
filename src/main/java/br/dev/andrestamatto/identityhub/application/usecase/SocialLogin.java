package br.dev.andrestamatto.identityhub.application.usecase;

import br.dev.andrestamatto.identityhub.application.result.AuthenticationResult;

public interface SocialLogin {
    AuthenticationResult execute(String provider, String authorizationCode, String redirectUri);
}
