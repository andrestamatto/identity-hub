package br.dev.andrestamatto.identityhub.clientapplication.domain;

import java.util.Objects;
import java.util.UUID;

public record ClientApplicationId(UUID value) {

    public ClientApplicationId {
        Objects.requireNonNull(value);
    }
}
