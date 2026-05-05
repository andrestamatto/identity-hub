package br.dev.andrestamatto.identityhub.application.usecase.dto;

public record SocialLoginCommand(
        String provider,
        String authorizationCode,
        String redirectUri
) {
}
