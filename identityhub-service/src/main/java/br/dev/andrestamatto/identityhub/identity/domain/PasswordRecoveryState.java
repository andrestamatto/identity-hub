package br.dev.andrestamatto.identityhub.identity.domain;

public enum PasswordRecoveryState {
    ACTIVE,
    USED,
    SUPERSEDED,
    EXPIRED,
    FAILED
}
