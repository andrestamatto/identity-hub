package br.dev.andrestamatto.identityhub.application.result;

public record AuthenticationResult(
        String accessToken,
        String tokenType,
        long expiresIn
) {}
