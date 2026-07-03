package br.dev.andrestamatto.identityhub.domain.valueobjects;

public enum UsernameType {
    EMAIL,
    PHONE;

    public static UsernameType create(String value) {
        return UsernameType.valueOf(value.toUpperCase());
    }
}
