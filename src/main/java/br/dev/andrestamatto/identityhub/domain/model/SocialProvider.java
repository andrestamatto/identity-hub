package br.dev.andrestamatto.identityhub.domain.model;

import lombok.Getter;

import java.util.Arrays;
import java.util.Locale;

@Getter
public enum SocialProvider {
    FACEBOOK("facebook"),
    GITHUB("github"),
    GOOGLE("google"),
    LINKEDIN("linkedin");

    private final String provider;

    SocialProvider(String provider) {
        this.provider = provider;
    }

    public static SocialProvider fromString(String provider) {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("Social provider is required");
        }
        String normalized = provider.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(value -> value.provider.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid social provider: " + provider));
    }

}
