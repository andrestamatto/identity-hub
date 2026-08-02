package br.dev.andrestamatto.identityhub.identity.application;

public final class PasswordRecoveryIdentityLookupException extends RuntimeException {

    public PasswordRecoveryIdentityLookupException(Throwable cause) {
        super("Identity provider could not evaluate password recovery", cause);
    }
}
