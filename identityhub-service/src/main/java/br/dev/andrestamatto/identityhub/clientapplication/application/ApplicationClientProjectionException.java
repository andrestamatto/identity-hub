package br.dev.andrestamatto.identityhub.clientapplication.application;

import java.util.Objects;

public final class ApplicationClientProjectionException extends RuntimeException {

    private final ApplicationClientProjectionFailureCode failureCode;
    private final boolean retryable;

    private ApplicationClientProjectionException(
            ApplicationClientProjectionFailureCode failureCode,
            boolean retryable,
            Throwable cause) {
        super(failureCode.name(), cause);
        this.failureCode = Objects.requireNonNull(failureCode);
        this.retryable = retryable;
    }

    public static ApplicationClientProjectionException retryable(
            ApplicationClientProjectionFailureCode failureCode,
            Throwable cause) {
        return new ApplicationClientProjectionException(failureCode, true, cause);
    }

    public static ApplicationClientProjectionException permanent(
            ApplicationClientProjectionFailureCode failureCode,
            Throwable cause) {
        return new ApplicationClientProjectionException(failureCode, false, cause);
    }

    public ApplicationClientProjectionFailureCode failureCode() {
        return failureCode;
    }

    public boolean retryable() {
        return retryable;
    }
}
