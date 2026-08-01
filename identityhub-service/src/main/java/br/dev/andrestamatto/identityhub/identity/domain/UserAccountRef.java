package br.dev.andrestamatto.identityhub.identity.domain;

import java.util.Objects;
import java.util.UUID;

public record UserAccountRef(UUID value) {

    public UserAccountRef {
        Objects.requireNonNull(value);
    }
}
