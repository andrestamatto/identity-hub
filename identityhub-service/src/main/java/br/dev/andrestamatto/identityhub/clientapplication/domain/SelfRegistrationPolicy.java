package br.dev.andrestamatto.identityhub.clientapplication.domain;

import java.util.Locale;

public enum SelfRegistrationPolicy {
    DISABLED,
    ENABLED;

    public static SelfRegistrationPolicy from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Self-registration policy is required");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported self-registration policy", exception);
        }
    }
}
