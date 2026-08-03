package br.dev.andrestamatto.identityhub.access.application;

public enum MembershipProjectionFailureCode {
    KEYCLOAK_UNAVAILABLE,
    KEYCLOAK_REJECTED,
    KEYCLOAK_INVALID_RESPONSE,
    MARKER_CONFLICT,
    USER_NOT_FOUND
}
