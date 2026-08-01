package br.dev.andrestamatto.identityhub.communication.application;

public enum EmailDeliveryResult {
    NO_WORK,
    DELIVERED,
    RETRY_SCHEDULED,
    FAILED
}
