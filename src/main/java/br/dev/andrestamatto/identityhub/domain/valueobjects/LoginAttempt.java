package br.dev.andrestamatto.identityhub.domain.valueobjects;

import br.dev.andrestamatto.identityhub.domain.entities.User;

import java.time.Instant;

/**
 * LoginAttempt representa um registro de tentativa de autenticação.
 * Exemplo: tentativa em 2026-05-15T10:00:00Z, IP 192.168.0.10, falha por credenciais inválidas.
 * Usado para auditoria e suporte a regras de bloqueio por tentativas.
 */
public record LoginAttempt(
        Instant attemptedAt,
        IPAddress ipAddress,
        User userAgent,
        boolean succeed,
        String reason
) {
}
