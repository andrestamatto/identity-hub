package br.dev.andrestamatto.identityhub.identity.application;

import java.util.Objects;

public final class EmailVerificationSecret {

    private final String value;

    public EmailVerificationSecret(String value) {
        this.value = Objects.requireNonNull(value);
        if (value.length() < 24 || value.length() > 128
                || !value.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Email verification secret is invalid");
        }
    }

    String value() {
        return value;
    }

    @Override
    public String toString() {
        return "EmailVerificationSecret[REDACTED]";
    }
}
