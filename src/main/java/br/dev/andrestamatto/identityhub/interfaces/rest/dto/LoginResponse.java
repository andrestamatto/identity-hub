package br.dev.andrestamatto.identityhub.interfaces.rest.dto;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresIn
) {}
