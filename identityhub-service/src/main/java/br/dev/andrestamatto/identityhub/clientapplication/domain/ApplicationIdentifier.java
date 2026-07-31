package br.dev.andrestamatto.identityhub.clientapplication.domain;

import java.util.regex.Pattern;

public record ApplicationIdentifier(String value) {

    private static final Pattern SUPPORTED_FORMAT =
            Pattern.compile("^[a-z0-9](?:[a-z0-9-]{1,61}[a-z0-9])$");

    public ApplicationIdentifier {
        if (value == null || !SUPPORTED_FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Application identifier must be a lowercase slug between 3 and 63 characters");
        }
    }
}
