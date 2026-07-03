package br.dev.andrestamatto.identityhub.infrastructure.apis;

public record WhatsappResponse(
        Boolean success,
        String to
) {}
