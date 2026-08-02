package br.dev.andrestamatto.identityhub.identity.application;

public final class LocalPasswordResetException extends RuntimeException {

    public LocalPasswordResetException() {
        super("Identity provider could not reset the local password");
    }
}
