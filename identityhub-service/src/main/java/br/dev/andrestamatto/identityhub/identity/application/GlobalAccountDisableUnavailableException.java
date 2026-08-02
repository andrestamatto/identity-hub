package br.dev.andrestamatto.identityhub.identity.application;

public final class GlobalAccountDisableUnavailableException extends RuntimeException {

    public GlobalAccountDisableUnavailableException() {
        super("Global account disable operation is temporarily unavailable");
    }
}
