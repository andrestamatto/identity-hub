package br.dev.andrestamatto.identityhub.communication.domain;

import java.util.Objects;
import java.util.UUID;

public record EmailDeliveryId(UUID value) {

    public EmailDeliveryId {
        Objects.requireNonNull(value);
    }
}
