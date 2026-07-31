package br.dev.andrestamatto.identityhub.clientapplication.application;

import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ApplicationClientProjection(
        UUID operationId,
        ApplicationClientId clientId,
        ApplicationClientProjectionState state,
        int attempts,
        Instant nextAttemptAt,
        String lastFailureCode,
        Instant createdAt,
        Instant updatedAt) {

    public ApplicationClientProjection {
        Objects.requireNonNull(operationId);
        Objects.requireNonNull(clientId);
        Objects.requireNonNull(state);
        Objects.requireNonNull(nextAttemptAt);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(updatedAt);
        if (attempts < 0) {
            throw new IllegalArgumentException("Projection attempts cannot be negative");
        }
    }

    public static ApplicationClientProjection pending(
            UUID operationId,
            ApplicationClientId clientId,
            Instant now) {
        return new ApplicationClientProjection(
                operationId,
                clientId,
                ApplicationClientProjectionState.PENDING,
                0,
                now,
                null,
                now,
                now);
    }
}
