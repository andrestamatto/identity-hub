package br.dev.andrestamatto.identityhub.identity.application;

public final class EmailVerificationRateLimitException extends RuntimeException {

    public EmailVerificationRateLimitException() {
        super("Email verification request limit reached");
    }
}
