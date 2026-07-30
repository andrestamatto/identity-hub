package br.dev.andrestamatto.identityhub.clientapplication.application;

public final class ClientApplicationConflictException extends RuntimeException {

    public ClientApplicationConflictException(String message) {
        super(message);
    }

    public ClientApplicationConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
