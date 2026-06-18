package br.dev.andrestamatto.identityhub.domain.valueobjects;

public record Username(
        String value,
        UsernameType usernameType
) {
    public Username {
        if (value == null || value.isBlank() || usernameType == null) {
            throw new IllegalArgumentException("Username value or type must not be null or blank.");
        }
    }

    public static Username create(String value) {
        return email(value);
    }

    public static Username create(String value, UsernameType type) {
        if (type == null) {
            throw new IllegalArgumentException("Username type must not be null.");
        }

        return switch (type) {
            case EMAIL -> email(value);
            case PHONE -> phone(value);
        };
    }

    public boolean isEmail() {
        return UsernameType.EMAIL.equals(usernameType);
    }

    public boolean isPhone() {
        return UsernameType.PHONE.equals(usernameType);
    }

    public static Username email(String value) {
        return new Username(value, UsernameType.EMAIL);
    }

    public static Username phone(String value) {
        return new Username(value, UsernameType.PHONE);
    }

}
