package br.dev.andrestamatto.identityhub.clientapplication.application;

import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientId;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationClientProjectionRepository {

    Optional<ApplicationClientConfiguration> reserveNext(
            UUID workerId,
            Instant now,
            Duration leaseDuration);

    void markApplied(UUID operationId, UUID workerId, Instant now);

    void scheduleRetry(
            UUID operationId,
            UUID workerId,
            int attempts,
            Instant nextAttemptAt,
            String failureCode,
            Instant now);

    void markFailed(
            UUID operationId,
            UUID workerId,
            int attempts,
            String failureCode,
            Instant now);

    Optional<ApplicationClientConfiguration> requeue(
            ApplicationClientId clientId,
            Instant now);

}
