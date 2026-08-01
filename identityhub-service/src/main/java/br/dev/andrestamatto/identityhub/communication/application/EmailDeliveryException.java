package br.dev.andrestamatto.identityhub.communication.application;

import java.util.Objects;

public final class EmailDeliveryException extends RuntimeException {

    private final EmailDeliveryFailureCode failureCode;
    private final boolean retryable;

    private EmailDeliveryException(
            EmailDeliveryFailureCode failureCode,
            boolean retryable,
            Throwable cause) {
        super("Email provider rejected delivery", cause);
        this.failureCode = Objects.requireNonNull(failureCode);
        this.retryable = retryable;
    }

    public static EmailDeliveryException retryable(
            EmailDeliveryFailureCode failureCode,
            Throwable cause) {
        return new EmailDeliveryException(failureCode, true, cause);
    }

    public static EmailDeliveryException permanent(
            EmailDeliveryFailureCode failureCode,
            Throwable cause) {
        return new EmailDeliveryException(failureCode, false, cause);
    }

    public EmailDeliveryFailureCode failureCode() {
        return failureCode;
    }

    public boolean retryable() {
        return retryable;
    }
}
