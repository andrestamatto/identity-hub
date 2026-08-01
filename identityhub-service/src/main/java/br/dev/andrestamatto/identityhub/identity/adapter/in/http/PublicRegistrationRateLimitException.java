package br.dev.andrestamatto.identityhub.identity.adapter.in.http;

public final class PublicRegistrationRateLimitException extends RuntimeException {

    private final long retryAfterSeconds;

    PublicRegistrationRateLimitException(long retryAfterSeconds) {
        super("Public registration request limit reached");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
