package br.dev.andrestamatto.identityhub.interfaces.rest.dto;

public record LoginRequest (
        String email,
        String password
) implements AuthenticatableRequest {}
