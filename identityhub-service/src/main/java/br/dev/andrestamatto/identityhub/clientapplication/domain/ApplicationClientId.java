package br.dev.andrestamatto.identityhub.clientapplication.domain;

import java.util.Objects;
import java.util.UUID;

public record ApplicationClientId(UUID value) {

    public ApplicationClientId {
        Objects.requireNonNull(value);
    }
}
