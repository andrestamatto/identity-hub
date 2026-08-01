package br.dev.andrestamatto.identityhub.communication.application;

public enum EmailDeliveryFailureCode {
    PROVIDER_UNAVAILABLE,
    PROVIDER_AUTHENTICATION_FAILED,
    INVALID_MESSAGE,
    UNEXPECTED_PROVIDER_FAILURE
}
