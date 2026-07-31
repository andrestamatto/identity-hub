package br.dev.andrestamatto.identityhub.clientapplication.application;

import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public record ApplicationClientProjection(
        UUID operationId,
        ApplicationClientId clientId,
        int payloadVersion,
        String correlationId,
        ApplicationClientProjectionState state,
        int attempts,
        Instant nextAttemptAt,
        String lastFailureCode,
        Instant createdAt,
        Instant updatedAt) {

    public ApplicationClientProjection {
        Objects.requireNonNull(operationId);
        Objects.requireNonNull(clientId);
        Objects.requireNonNull(correlationId);
        Objects.requireNonNull(state);
        Objects.requireNonNull(nextAttemptAt);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(updatedAt);
        if (attempts < 0) {
            throw new IllegalArgumentException("Projection attempts cannot be negative");
        }
        if (payloadVersion != 1) {
            throw new IllegalArgumentException("Unsupported projection payload version");
        }
        if (!Pattern.matches("[A-Za-z0-9._-]{1,64}", correlationId)) {
            throw new IllegalArgumentException("Invalid projection correlation id");
        }
    }

    public static ApplicationClientProjection pending(
            UUID operationId,
            ApplicationClientId clientId,
            String correlationId,
            Instant now) {
        return new ApplicationClientProjection(
                operationId,
                clientId,
                1,
                correlationId,
                ApplicationClientProjectionState.PENDING,
                0,
                now,
                null,
                now,
                now);
    }
}
