package br.dev.andrestamatto.identityhub.identity.adapter.in.http;

public final class PublicPasswordRecoveryRateLimitException extends RuntimeException {

    private final long retryAfterSeconds;

    PublicPasswordRecoveryRateLimitException(long retryAfterSeconds) {
        super("Password recovery request limit reached");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
