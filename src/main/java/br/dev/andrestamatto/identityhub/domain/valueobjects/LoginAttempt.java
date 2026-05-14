package br.dev.andrestamatto.identityhub.domain.valueobjects;

import br.dev.andrestamatto.identityhub.domain.entities.User;

import java.time.Instant;

public record LoginAttempt(
        Instant attemptedAt,
        IPAddress ipAddress,
        User userAgent,
        boolean succeed,
        String reason
) {
}
