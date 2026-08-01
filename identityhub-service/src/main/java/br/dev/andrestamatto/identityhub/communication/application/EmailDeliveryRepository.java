package br.dev.andrestamatto.identityhub.communication.application;

import br.dev.andrestamatto.identityhub.communication.domain.EmailDeliveryId;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EmailDeliveryRepository {

    Optional<EmailDelivery> find(EmailDeliveryId id);

    void add(EmailDelivery delivery);

    Optional<EmailDelivery> reserveNext(UUID workerId, Instant now, Duration lease);

    void markDelivered(EmailDeliveryId id, UUID workerId, Instant now);

    void scheduleRetry(
            EmailDeliveryId id,
            UUID workerId,
            int attempts,
            Instant nextAttemptAt,
            String failureCode,
            Instant now);

    void markFailed(
            EmailDeliveryId id,
            UUID workerId,
            int attempts,
            String failureCode,
            Instant now);

    Optional<EmailDelivery> requeue(EmailDeliveryId id, Instant now);
}
