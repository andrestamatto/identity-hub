package br.dev.andrestamatto.identityhub.infrastructure.security.config;

public enum AccessType {
    PERMIT_ALL,
    DENY_ALL,
    AUTHENTICATED,
    ROLE,
    PERM,
    ANY_ROLE,
    ANY_PERM,
    ANY_AUTHORITY,
    ALL_ROLE,
    ALL_PERM,
    HAS_IP
}
