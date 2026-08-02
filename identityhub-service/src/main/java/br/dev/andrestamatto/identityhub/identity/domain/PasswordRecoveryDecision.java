package br.dev.andrestamatto.identityhub.identity.domain;

public enum PasswordRecoveryDecision {
    VALID,
    INVALID,
    EXPIRED,
    INACTIVE
}
