package br.dev.andrestamatto.identityhub.domain.model;

import java.util.Objects;

public class Password {

    private String value;

    public Password(String value) {
        this.value = value;
    }

    public static Password raw(String rawPassword) {
        return new Password(rawPassword);
    }

    public static Password encoded(String encodedPassword) {
        return new Password(encodedPassword);
    }

    public String getValue() {
        return this.value;
    }

    public boolean equals(Password other) {
        return Objects.equals(this.value, other.value);
    }
}
