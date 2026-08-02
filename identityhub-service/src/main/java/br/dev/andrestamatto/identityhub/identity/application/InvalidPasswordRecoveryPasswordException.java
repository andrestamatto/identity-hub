package br.dev.andrestamatto.identityhub.identity.application;

public final class InvalidPasswordRecoveryPasswordException extends RuntimeException {

    public InvalidPasswordRecoveryPasswordException() {
        super("New password does not satisfy the recovery policy");
    }
}
