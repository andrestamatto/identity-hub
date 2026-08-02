package br.dev.andrestamatto.identityhub.identity.application;

public final class GlobalAccountDisableConflictException extends RuntimeException {

    public GlobalAccountDisableConflictException() {
        super("Idempotency key was already used for another command");
    }
}
