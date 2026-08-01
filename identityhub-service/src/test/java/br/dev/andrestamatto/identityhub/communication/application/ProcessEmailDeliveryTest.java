package br.dev.andrestamatto.identityhub.communication.application;

import static org.assertj.core.api.Assertions.assertThat;

import br.dev.andrestamatto.identityhub.communication.domain.EmailDeliveryId;
import br.dev.andrestamatto.identityhub.communication.domain.EmailRecipient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProcessEmailDeliveryTest {

    private static final Instant NOW = Instant.parse("2026-07-31T18:00:00Z");
    private static final UUID WORKER_ID = UUID.fromString("87ba5cc1-ae98-4a85-aeb0-103103d5bd23");

    @Test
    void marksSuccessfulDelivery() {
        var repository = new RecordingRepository(delivery(0));
        var useCase = useCase(repository, message -> { });

        assertThat(useCase.processNext(WORKER_ID)).isEqualTo(EmailDeliveryResult.DELIVERED);
        assertThat(repository.transition).isEqualTo("DELIVERED");
    }

    @Test
    void retriesTemporaryFailureWithBackoff() {
        var repository = new RecordingRepository(delivery(1));
        var useCase = useCase(repository, message -> {
            throw EmailDeliveryException.retryable(
                    EmailDeliveryFailureCode.PROVIDER_UNAVAILABLE,
                    new IllegalStateException("synthetic"));
        });

        assertThat(useCase.processNext(WORKER_ID)).isEqualTo(EmailDeliveryResult.RETRY_SCHEDULED);
        assertThat(repository.attempts).isEqualTo(2);
        assertThat(repository.nextAttemptAt).isEqualTo(NOW.plusSeconds(20));
    }

    @Test
    void failsPermanentErrorWithoutRetry() {
        var repository = new RecordingRepository(delivery(0));
        var useCase = useCase(repository, message -> {
            throw EmailDeliveryException.permanent(
                    EmailDeliveryFailureCode.INVALID_MESSAGE,
                    new IllegalArgumentException("synthetic"));
        });

        assertThat(useCase.processNext(WORKER_ID)).isEqualTo(EmailDeliveryResult.FAILED);
        assertThat(repository.transition).isEqualTo("FAILED");
        assertThat(repository.attempts).isOne();
    }

    private ProcessEmailDelivery useCase(EmailDeliveryRepository repository, EmailDeliverySender sender) {
        return new ProcessEmailDelivery(
                repository,
                sender,
                new PasswordChangedEmailRenderer(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofSeconds(30),
                Duration.ofSeconds(10),
                3);
    }

    private EmailDelivery delivery(int attempts) {
        return EmailDelivery.reconstitute(
                new EmailDeliveryId(UUID.fromString("27f3aa0b-6a70-43bd-a087-d5bc0c1bc779")),
                UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0"),
                "auto-radar",
                "Auto Radar",
                "development",
                new EmailRecipient("andre@example.com"),
                EmailDeliveryPurpose.PASSWORD_CHANGED,
                EmailDeliveryState.PENDING,
                attempts,
                NOW,
                null,
                "correlation-123",
                NOW.minusSeconds(60),
                NOW.minusSeconds(60));
    }

    private static final class RecordingRepository implements EmailDeliveryRepository {
        private final EmailDelivery delivery;
        private String transition;
        private int attempts;
        private Instant nextAttemptAt;

        private RecordingRepository(EmailDelivery delivery) {
            this.delivery = delivery;
        }

        @Override
        public Optional<EmailDelivery> find(EmailDeliveryId id) {
            return Optional.ofNullable(delivery);
        }

        @Override
        public void add(EmailDelivery value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<EmailDelivery> reserveNext(UUID workerId, Instant now, Duration lease) {
            return Optional.ofNullable(delivery);
        }

        @Override
        public void markDelivered(EmailDeliveryId id, UUID workerId, Instant now) {
            transition = "DELIVERED";
        }

        @Override
        public void scheduleRetry(EmailDeliveryId id, UUID workerId, int attempts,
                Instant nextAttemptAt, String failureCode, Instant now) {
            transition = "RETRY";
            this.attempts = attempts;
            this.nextAttemptAt = nextAttemptAt;
        }

        @Override
        public void markFailed(EmailDeliveryId id, UUID workerId, int attempts,
                String failureCode, Instant now) {
            transition = "FAILED";
            this.attempts = attempts;
        }

        @Override
        public Optional<EmailDelivery> requeue(EmailDeliveryId id, Instant now) {
            throw new UnsupportedOperationException();
        }
    }
}
