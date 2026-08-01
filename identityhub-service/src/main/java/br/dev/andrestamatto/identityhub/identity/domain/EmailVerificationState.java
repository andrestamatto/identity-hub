package br.dev.andrestamatto.identityhub.identity.domain;

public enum EmailVerificationState {
    ACTIVE,
    USED,
    SUPERSEDED,
    EXPIRED,
    FAILED
}
