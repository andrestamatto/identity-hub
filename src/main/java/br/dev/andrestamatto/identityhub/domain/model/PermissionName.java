package br.dev.andrestamatto.identityhub.domain.model;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record PermissionName(String value) {

    private static final Pattern VALID_PATTERN = Pattern.compile("^[A-Z0-9_]+$");

    public PermissionName {
        Objects.requireNonNull(value, "Permission value must not be null");
        value = value.trim().toUpperCase(Locale.ROOT);
        if (value.isBlank() || !VALID_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid permission value: " + value);
        }
    }

    public static PermissionName from(String value) {
        return new PermissionName(value);
    }
}
