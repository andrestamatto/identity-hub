package br.dev.andrestamatto.identityhub.identity.application;

public final class SelfRegistrationDisabledException extends RuntimeException {

    public SelfRegistrationDisabledException() {
        super("Self-registration is disabled for this application");
    }
}
