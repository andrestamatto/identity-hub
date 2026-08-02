package br.dev.andrestamatto.identityhub.identity.domain;

import java.util.Objects;
import java.util.regex.Pattern;

public record PkceCodeChallenge(String value) {

    private static final Pattern S256_FORMAT = Pattern.compile("[A-Za-z0-9_-]{43}");

    public PkceCodeChallenge {
        Objects.requireNonNull(value);
        if (!S256_FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid PKCE S256 code challenge");
        }
    }
}
