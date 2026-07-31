package br.dev.andrestamatto.identityhub.clientapplication.application;

import java.util.UUID;

public final class ApplicationClientNotFoundException extends RuntimeException {

    public ApplicationClientNotFoundException(UUID clientId) {
        super("Application client not found: " + clientId);
    }
}
