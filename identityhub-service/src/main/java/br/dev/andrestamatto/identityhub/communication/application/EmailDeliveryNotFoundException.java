package br.dev.andrestamatto.identityhub.communication.application;

import java.util.UUID;

public final class EmailDeliveryNotFoundException extends RuntimeException {

    public EmailDeliveryNotFoundException(UUID deliveryId) {
        super("Email delivery does not exist: " + deliveryId);
    }
}
