package br.dev.andrestamatto.identityhub.identity.domain;

import java.net.IDN;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

public final class LoginEmail {

    private static final int MAX_LENGTH = 254;

    private final String contactValue;
    private final String normalizedValue;

    public LoginEmail(String value) {
        Objects.requireNonNull(value);
        contactValue = Normalizer.normalize(value.trim(), Normalizer.Form.NFC);
        var separator = contactValue.indexOf('@');
        if (contactValue.isEmpty()
                || contactValue.length() > MAX_LENGTH
                || separator < 1
                || separator != contactValue.lastIndexOf('@')
                || separator == contactValue.length() - 1
                || contactValue.chars().anyMatch(Character::isWhitespace)
                || contactValue.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Login email is invalid");
        }
        var localPart = contactValue.substring(0, separator).toLowerCase(Locale.ROOT);
        var domain = asciiDomain(contactValue.substring(separator + 1));
        normalizedValue = localPart + "@" + domain;
        if (normalizedValue.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Login email is invalid");
        }
    }

    public String contactValue() {
        return contactValue;
    }

    public String normalizedValue() {
        return normalizedValue;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof LoginEmail email
                && normalizedValue.equals(email.normalizedValue);
    }

    @Override
    public int hashCode() {
        return normalizedValue.hashCode();
    }

    @Override
    public String toString() {
        return "LoginEmail[REDACTED]";
    }

    private static String asciiDomain(String domain) {
        try {
            var normalized = IDN.toASCII(domain, IDN.USE_STD3_ASCII_RULES)
                    .toLowerCase(Locale.ROOT);
            if (normalized.isBlank() || !normalized.contains(".")) {
                throw new IllegalArgumentException("Login email is invalid");
            }
            return normalized;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Login email is invalid", exception);
        }
    }
}
