package br.dev.andrestamatto.identityhub.access.application;

import java.util.Objects;

public final class MembershipProjectionException extends RuntimeException {

    private final MembershipProjectionFailureCode failureCode;
    private final boolean retryable;

    private MembershipProjectionException(
            MembershipProjectionFailureCode failureCode,
            boolean retryable,
            Throwable cause) {
        super(failureCode.name(), cause);
        this.failureCode = Objects.requireNonNull(failureCode);
        this.retryable = retryable;
    }

    public static MembershipProjectionException retryable(
            MembershipProjectionFailureCode failureCode, Throwable cause) {
        return new MembershipProjectionException(failureCode, true, cause);
    }

    public static MembershipProjectionException permanent(
            MembershipProjectionFailureCode failureCode, Throwable cause) {
        return new MembershipProjectionException(failureCode, false, cause);
    }

    public MembershipProjectionFailureCode failureCode() {
        return failureCode;
    }

    public boolean retryable() {
        return retryable;
    }
}
