package br.dev.andrestamatto.identityhub.clientapplication.application;

import java.util.Objects;

public final class ConfidentialClientSecret {

    private final String value;

    public ConfidentialClientSecret(String value) {
        this.value = Objects.requireNonNull(value);
        if (value.isBlank()) {
            throw new IllegalArgumentException("Confidential client secret cannot be blank");
        }
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return "[REDACTED]";
    }
}
