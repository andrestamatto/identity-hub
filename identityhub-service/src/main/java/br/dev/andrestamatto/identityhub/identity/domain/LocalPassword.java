package br.dev.andrestamatto.identityhub.identity.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class LocalPassword implements AutoCloseable {

    private static final int MIN_CODE_POINTS = 15;
    private static final int MAX_CODE_POINTS = 64;
    private static final Set<String> BLOCKED = Set.of(
            "passwordpassword",
            "123456789012345",
            "qwertyqwertyqwerty",
            "senha1234567890");

    private final char[] value;

    public LocalPassword(char[] value) {
        Objects.requireNonNull(value);
        this.value = value.clone();
        var text = new String(this.value);
        var length = text.codePointCount(0, text.length());
        if (length < MIN_CODE_POINTS || length > MAX_CODE_POINTS
                || BLOCKED.contains(text.toLowerCase(Locale.ROOT))) {
            close();
            throw new IllegalArgumentException("Password does not satisfy the security policy");
        }
    }

    public char[] copy() {
        return value.clone();
    }

    @Override
    public void close() {
        Arrays.fill(value, '\0');
    }

    @Override
    public String toString() {
        return "LocalPassword[REDACTED]";
    }
}
