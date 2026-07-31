package br.dev.andrestamatto.identityhub.clientapplication.domain;

public record DisplayName(String value) {

    private static final int MAXIMUM_LENGTH = 120;

    public DisplayName {
        if (value == null) {
            throw new IllegalArgumentException("Display name is required");
        }
        value = value.strip();
        if (value.isEmpty() || value.length() > MAXIMUM_LENGTH) {
            throw new IllegalArgumentException(
                    "Display name must have between 1 and 120 characters");
        }
    }
}
