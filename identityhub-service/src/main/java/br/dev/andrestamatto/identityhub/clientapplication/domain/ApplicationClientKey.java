package br.dev.andrestamatto.identityhub.clientapplication.domain;

import java.util.regex.Pattern;

public record ApplicationClientKey(String value) {

    private static final Pattern SUPPORTED_FORMAT =
            Pattern.compile("^[a-z0-9](?:[a-z0-9-]{1,61}[a-z0-9])$");

    public ApplicationClientKey {
        if (value == null || !SUPPORTED_FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Application client key must be a lowercase slug between 3 and 63 characters");
        }
    }
}
