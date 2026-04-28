package br.dev.andrestamatto.identityhub.domain.model;

public record RawPassword(String value) {
    public RawPassword {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Password is required");
    }
    public static RawPassword from(String value) { return new RawPassword(value); }
}
