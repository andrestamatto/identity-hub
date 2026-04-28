package br.dev.andrestamatto.identityhub.domain.model;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record RoleName(String value) {

    private static final Pattern VALID_PATTERN = Pattern.compile("^[A-Z0-9_]+$");

    public RoleName {
        Objects.requireNonNull(value, "Role value must not be null");
        value = value.trim().toUpperCase(Locale.ROOT);
        if (value.isBlank() || !VALID_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid role value: " + value);
        }
    }

    public static RoleName from(String value) {
        return new RoleName(value);
    }
}
