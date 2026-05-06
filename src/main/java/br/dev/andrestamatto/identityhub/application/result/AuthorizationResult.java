package br.dev.andrestamatto.identityhub.application.result;

import java.net.URI;

public record AuthorizationResult(
        URI authorizationURI,
        String sessionState
) {}
