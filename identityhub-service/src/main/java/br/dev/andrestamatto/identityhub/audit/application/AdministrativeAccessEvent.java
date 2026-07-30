package br.dev.andrestamatto.identityhub.audit.application;

import java.time.Instant;
import java.util.UUID;

public record AdministrativeAccessEvent(
        UUID id,
        Instant occurredAt,
        String correlationId,
        String actorSubject,
        String method,
        String path,
        AdministrativeAccessOutcome outcome,
        String reason) {
}
