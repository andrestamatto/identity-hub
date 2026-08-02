package br.dev.andrestamatto.identityhub.identity.domain;

import java.util.Objects;
import java.util.regex.Pattern;

public record OnboardingSessionId(String value) {

    private static final Pattern FORMAT = Pattern.compile("[A-Za-z0-9_-]{43}");

    public OnboardingSessionId {
        Objects.requireNonNull(value);
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid onboarding session id");
        }
    }
}
