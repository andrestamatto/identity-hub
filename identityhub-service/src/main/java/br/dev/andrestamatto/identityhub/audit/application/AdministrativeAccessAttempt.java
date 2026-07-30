package br.dev.andrestamatto.identityhub.audit.application;

import java.util.Objects;

public record AdministrativeAccessAttempt(
        String correlationId,
        String actorSubject,
        String method,
        String path,
        AdministrativeAccessOutcome outcome,
        String reason) {

    public AdministrativeAccessAttempt {
        Objects.requireNonNull(correlationId);
        Objects.requireNonNull(method);
        Objects.requireNonNull(path);
        Objects.requireNonNull(outcome);
        Objects.requireNonNull(reason);
    }
}
