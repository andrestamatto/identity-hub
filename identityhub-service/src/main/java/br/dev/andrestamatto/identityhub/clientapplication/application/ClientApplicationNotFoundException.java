package br.dev.andrestamatto.identityhub.clientapplication.application;

import java.util.UUID;

public final class ClientApplicationNotFoundException extends RuntimeException {

    public ClientApplicationNotFoundException(UUID applicationId) {
        super("Client application not found: " + applicationId);
    }
}
