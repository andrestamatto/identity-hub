package br.dev.andrestamatto.identityhub.clientapplication.application;

public final class ClientApplicationUnavailableException extends RuntimeException {

    public ClientApplicationUnavailableException() {
        super("Client application is unavailable");
    }
}
