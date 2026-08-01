package br.dev.andrestamatto.identityhub.identity.application;

public final class LocalIdentityVerificationException extends RuntimeException {

    private final boolean retryable;

    public LocalIdentityVerificationException(boolean retryable) {
        super("Identity provider could not complete email verification");
        this.retryable = retryable;
    }

    public boolean retryable() {
        return retryable;
    }
}
