package br.dev.andrestamatto.identityhub.identity.application;

public final class PasswordRecoveryRejectedException extends RuntimeException {

    public PasswordRecoveryRejectedException() {
        super("Password recovery proof was rejected");
    }
}
