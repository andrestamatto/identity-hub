package br.dev.andrestamatto.identityhub.identity.application;

public final class EmailVerificationRejectedException extends RuntimeException {

    public EmailVerificationRejectedException() {
        super("Email verification could not be completed");
    }
}
