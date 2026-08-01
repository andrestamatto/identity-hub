package br.dev.andrestamatto.identityhub.communication.application;

public final class EmailDeliveryConflictException extends RuntimeException {

    public EmailDeliveryConflictException() {
        super("Email delivery identifier is already assigned to another request");
    }
}
