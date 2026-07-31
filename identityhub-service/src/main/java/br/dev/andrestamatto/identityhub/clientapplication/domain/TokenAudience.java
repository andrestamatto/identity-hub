package br.dev.andrestamatto.identityhub.clientapplication.domain;

import java.util.regex.Pattern;

public record TokenAudience(String value) {

    private static final int MAXIMUM_LENGTH = 255;
    private static final Pattern SUPPORTED_FORMAT =
            Pattern.compile("^[A-Za-z0-9](?:[A-Za-z0-9._:/-]*[A-Za-z0-9])$");

    public TokenAudience {
        if (value == null
                || value.length() > MAXIMUM_LENGTH
                || !SUPPORTED_FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Token audience must be an exact safe identifier between 2 and 255 characters");
        }
    }
}
