package br.dev.andrestamatto.identityhub.application.usecase;

public record SocialLoginInput(
        String provider,
        String authorizationCode,
        String redirectUri
) {
}
