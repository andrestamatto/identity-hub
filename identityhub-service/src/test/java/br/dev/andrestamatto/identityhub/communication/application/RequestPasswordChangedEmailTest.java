package br.dev.andrestamatto.identityhub.communication.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.dev.andrestamatto.identityhub.communication.domain.EmailDeliveryId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RequestPasswordChangedEmailTest {

    private static final Instant NOW = Instant.parse("2026-07-31T18:00:00Z");
    private static final UUID APPLICATION_ID = UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0");
    private static final UUID DELIVERY_ID = UUID.fromString("27f3aa0b-6a70-43bd-a087-d5bc0c1bc779");

    @Test
    void persistsResolvedOriginBeforeDelivery() {
        var repository = new RecordingRepository();
        var useCase = useCase(repository);

        var result = useCase.execute(command("andre@example.com"));

        assertThat(result.created()).isTrue();
        assertThat(repository.find(new EmailDeliveryId(DELIVERY_ID)).orElseThrow())
                .satisfies(delivery -> {
                    assertThat(delivery.applicationIdentifier()).isEqualTo("auto-radar");
                    assertThat(delivery.applicationDisplayName()).isEqualTo("Auto Radar");
                    assertThat(delivery.environment()).isEqualTo("development");
                    assertThat(delivery.purpose()).isEqualTo(EmailDeliveryPurpose.PASSWORD_CHANGED);
                });
    }

    @Test
    void identicalReplayReturnsExistingDelivery() {
        var repository = new RecordingRepository();
        var useCase = useCase(repository);

        useCase.execute(command("andre@example.com"));
        var replay = useCase.execute(command("andre@example.com"));

        assertThat(replay.created()).isFalse();
        assertThat(repository.size()).isOne();
    }

    @Test
    void conflictingReplayIsRejected() {
        var repository = new RecordingRepository();
        var useCase = useCase(repository);
        useCase.execute(command("andre@example.com"));

        assertThatThrownBy(() -> useCase.execute(command("other@example.com")))
                .isInstanceOf(EmailDeliveryConflictException.class);
    }

    @Test
    void concurrentIdenticalInsertIsTreatedAsReplay() {
        var winner = new RecordingRepository();
        useCase(winner).execute(command("andre@example.com"));
        var racedDelivery = winner.find(new EmailDeliveryId(DELIVERY_ID)).orElseThrow();
        var repository = new RecordingRepository();
        repository.simulateConcurrentInsert(racedDelivery);

        var result = useCase(repository).execute(command("andre@example.com"));

        assertThat(result.created()).isFalse();
        assertThat(repository.size()).isOne();
    }

    private RequestPasswordChangedEmail useCase(RecordingRepository repository) {
        EmailOriginResolver resolver = id -> new EmailOrigin(
                id, "auto-radar", "Auto Radar", "development");
        return new RequestPasswordChangedEmail(
                repository,
                resolver,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private RequestPasswordChangedEmail.Command command(String recipient) {
        return new RequestPasswordChangedEmail.Command(
                DELIVERY_ID,
                APPLICATION_ID,
                recipient,
                "correlation-123");
    }

    private static final class RecordingRepository implements EmailDeliveryRepository {
        private final Map<EmailDeliveryId, EmailDelivery> deliveries = new HashMap<>();
        private EmailDelivery concurrentInsert;

        @Override
        public Optional<EmailDelivery> find(EmailDeliveryId id) {
            return Optional.ofNullable(deliveries.get(id));
        }

        @Override
        public void add(EmailDelivery delivery) {
            if (concurrentInsert != null) {
                deliveries.put(concurrentInsert.id(), concurrentInsert);
                throw new EmailDeliveryConflictException();
            }
            deliveries.put(delivery.id(), delivery);
        }

        @Override
        public Optional<EmailDelivery> reserveNext(UUID workerId, Instant now, java.time.Duration lease) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void markDelivered(EmailDeliveryId id, UUID workerId, Instant now) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void scheduleRetry(EmailDeliveryId id, UUID workerId, int attempts,
                Instant nextAttemptAt, String failureCode, Instant now) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void markFailed(EmailDeliveryId id, UUID workerId, int attempts,
                String failureCode, Instant now) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<EmailDelivery> requeue(EmailDeliveryId id, Instant now) {
            throw new UnsupportedOperationException();
        }

        int size() {
            return deliveries.size();
        }

        void simulateConcurrentInsert(EmailDelivery delivery) {
            concurrentInsert = delivery;
        }
    }
}
