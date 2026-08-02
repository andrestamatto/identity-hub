package br.dev.andrestamatto.identityhub.identity.domain;

import java.util.Objects;
import java.util.regex.Pattern;

public record OnboardingDigest(String value) {

    private static final Pattern FORMAT = Pattern.compile("[0-9a-f]{64}");

    public OnboardingDigest {
        Objects.requireNonNull(value);
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid onboarding digest");
        }
    }
}
