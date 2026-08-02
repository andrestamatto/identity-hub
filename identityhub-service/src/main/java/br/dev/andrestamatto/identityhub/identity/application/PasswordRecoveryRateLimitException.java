package br.dev.andrestamatto.identityhub.identity.application;

public final class PasswordRecoveryRateLimitException extends RuntimeException {

    public PasswordRecoveryRateLimitException() {
        super("Password recovery request limit reached");
    }
}
