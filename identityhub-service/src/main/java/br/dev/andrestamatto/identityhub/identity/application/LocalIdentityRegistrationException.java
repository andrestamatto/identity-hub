package br.dev.andrestamatto.identityhub.identity.application;

import java.util.Objects;

public final class LocalIdentityRegistrationException extends RuntimeException {

    private final LocalIdentityRegistrationFailureCode failureCode;
    private final boolean retryable;

    private LocalIdentityRegistrationException(
            LocalIdentityRegistrationFailureCode failureCode,
            boolean retryable,
            Throwable cause) {
        super("Identity provider could not complete registration", cause);
        this.failureCode = Objects.requireNonNull(failureCode);
        this.retryable = retryable;
    }

    public static LocalIdentityRegistrationException retryable(
            LocalIdentityRegistrationFailureCode failureCode,
            Throwable cause) {
        return new LocalIdentityRegistrationException(failureCode, true, cause);
    }

    public static LocalIdentityRegistrationException permanent(
            LocalIdentityRegistrationFailureCode failureCode,
            Throwable cause) {
        return new LocalIdentityRegistrationException(failureCode, false, cause);
    }

    public LocalIdentityRegistrationFailureCode failureCode() {
        return failureCode;
    }

    public boolean retryable() {
        return retryable;
    }
}
