package br.dev.andrestamatto.identityhub.clientapplication.application;

public enum ApplicationClientProjectionFailureCode {
    KEYCLOAK_UNAVAILABLE,
    KEYCLOAK_MANAGEMENT_AUTH_REJECTED,
    KEYCLOAK_MANAGEMENT_REJECTED,
    KEYCLOAK_INVALID_RESPONSE,
    KEYCLOAK_CLIENT_CONFLICT
}
