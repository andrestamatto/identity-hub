package br.dev.andrestamatto.identityhub.domain.valueobjects;

public record Username(
        String value,
        UsernameType usernameType
) {
    public Username {
        if (value == null || usernameType == null) { throw new IllegalArgumentException("Username value or type must not be null"); }
        if (!usernameType.validate(value)) { throw new IllegalArgumentException("Invalid username value"); }
    }

    public Username(String value) {
        this(value, UsernameType.EMAIL);
    }

}
