package br.dev.andrestamatto.identityhub.domain.model;

public record EncodedPassword(String value) {
    public EncodedPassword {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Encoded password is required");
    }
    public static EncodedPassword from(String value) { return new EncodedPassword(value); }
}
