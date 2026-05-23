package br.dev.andrestamatto.identityhub.domain.valueobjects;

public record Username(
        String value,
        UsernameType usernameType
) {
    public Username {
        if (value == null || usernameType == null) { throw new IllegalArgumentException("Username value or type must not be null"); }
        if (!usernameType.validate(value)) { throw new IllegalArgumentException("Invalid username value"); }
    }

    // Default type: EMAIL
    public static Username create(String value) {
        return Username.create(value, UsernameType.EMAIL);
    }

    public static Username create(String value, UsernameType type) {
        return new Username(value, type);
    }

}
