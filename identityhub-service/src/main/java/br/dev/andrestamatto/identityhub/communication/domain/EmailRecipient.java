package br.dev.andrestamatto.identityhub.communication.domain;

import java.util.Objects;

public record EmailRecipient(String value) {

    private static final int MAX_LENGTH = 254;

    public EmailRecipient {
        Objects.requireNonNull(value);
        value = value.trim();
        var separator = value.indexOf('@');
        if (value.isEmpty()
                || value.length() > MAX_LENGTH
                || separator < 1
                || separator != value.lastIndexOf('@')
                || separator == value.length() - 1
                || value.chars().anyMatch(Character::isWhitespace)
                || value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Email address is invalid");
        }
    }
}
