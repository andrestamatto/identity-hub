package br.dev.andrestamatto.identityhub.domain.valueobjects;

/**
 * UserStatus representa o estado de autenticação/uso da conta.
 * Exemplo: ACTIVE (pode autenticar), LOCKED (bloqueado temporariamente),
 * DISABLED (desativado), PENDING_VERIFICATION (aguardando validação).
 */
public enum UserStatus {
    ACTIVE, LOCKED, DISABLED, PENDING_VERIFICATION
}
