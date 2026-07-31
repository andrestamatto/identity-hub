package br.dev.andrestamatto.identityhub.clientapplication.application;

import static org.assertj.core.api.Assertions.assertThat;

import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClient;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientKey;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientType;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplicationId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.TokenAudience;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProcessApplicationClientProjectionTest {

    private static final Instant NOW = Instant.parse("2026-07-31T16:00:00Z");
    private static final UUID WORKER_ID = UUID.fromString("87ba5cc1-ae98-4a85-aeb0-103103d5bd23");

    @Test
    void appliesReservedProjection() {
        var repository = new RecordingRepository(configuration(0));
        var projected = new boolean[1];
        var useCase = useCase(repository, client -> projected[0] = true);

        assertThat(useCase.processNext(WORKER_ID))
                .isEqualTo(ApplicationClientProjectionResult.APPLIED);
        assertThat(projected[0]).isTrue();
        assertThat(repository.transition).isEqualTo("APPLIED");
    }

    @Test
    void schedulesRetryWithExponentialBackoffForTransientFailure() {
        var repository = new RecordingRepository(configuration(1));
        var useCase = useCase(repository, client -> {
            throw ApplicationClientProjectionException.retryable(
                    ApplicationClientProjectionFailureCode.KEYCLOAK_UNAVAILABLE,
                    new IllegalStateException("synthetic"));
        });

        assertThat(useCase.processNext(WORKER_ID))
                .isEqualTo(ApplicationClientProjectionResult.RETRY_SCHEDULED);
        assertThat(repository.transition).isEqualTo("RETRY");
        assertThat(repository.attempts).isEqualTo(2);
        assertThat(repository.nextAttemptAt).isEqualTo(NOW.plusSeconds(20));
        assertThat(repository.failureCode).isEqualTo("KEYCLOAK_UNAVAILABLE");
    }

    @Test
    void failsImmediatelyForPermanentFailure() {
        var repository = new RecordingRepository(configuration(0));
        var useCase = useCase(repository, client -> {
            throw ApplicationClientProjectionException.permanent(
                    ApplicationClientProjectionFailureCode.KEYCLOAK_CLIENT_CONFLICT,
                    new IllegalStateException("synthetic"));
        });

        assertThat(useCase.processNext(WORKER_ID))
                .isEqualTo(ApplicationClientProjectionResult.FAILED);
        assertThat(repository.transition).isEqualTo("FAILED");
        assertThat(repository.attempts).isOne();
    }

    @Test
    void failsAfterMaximumTransientAttempts() {
        var repository = new RecordingRepository(configuration(2));
        var useCase = useCase(repository, client -> {
            throw ApplicationClientProjectionException.retryable(
                    ApplicationClientProjectionFailureCode.KEYCLOAK_UNAVAILABLE,
                    new IllegalStateException("synthetic"));
        });

        assertThat(useCase.processNext(WORKER_ID))
                .isEqualTo(ApplicationClientProjectionResult.FAILED);
        assertThat(repository.transition).isEqualTo("FAILED");
        assertThat(repository.attempts).isEqualTo(3);
    }

    @Test
    void reportsWhenNoProjectionIsDue() {
        var repository = new RecordingRepository(null);

        assertThat(useCase(repository, client -> { }).processNext(WORKER_ID))
                .isEqualTo(ApplicationClientProjectionResult.NO_WORK);
        assertThat(repository.transition).isNull();
    }

    private ProcessApplicationClientProjection useCase(
            ApplicationClientProjectionRepository repository,
            ApplicationClientProjector projector) {
        return new ProcessApplicationClientProjection(
                repository,
                projector,
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofSeconds(30),
                Duration.ofSeconds(10),
                3);
    }

    private ApplicationClientConfiguration configuration(int attempts) {
        var clientId = new ApplicationClientId(
                UUID.fromString("ff7c4748-f053-4fb6-91be-d34cf0015834"));
        var client = ApplicationClient.reconstitute(
                clientId,
                new ClientApplicationId(
                        UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0")),
                new ApplicationClientKey("catalog-api"),
                ApplicationClientType.API,
                new TokenAudience("catalog-api"),
                true,
                NOW.minusSeconds(60));
        var projection = new ApplicationClientProjection(
                UUID.fromString("27f3aa0b-6a70-43bd-a087-d5bc0c1bc779"),
                clientId,
                ApplicationClientProjectionState.PENDING,
                attempts,
                NOW,
                null,
                NOW.minusSeconds(60),
                NOW.minusSeconds(60));
        return new ApplicationClientConfiguration(client, projection);
    }

    private static final class RecordingRepository
            implements ApplicationClientProjectionRepository {

        private final ApplicationClientConfiguration configuration;
        private String transition;
        private int attempts;
        private Instant nextAttemptAt;
        private String failureCode;

        private RecordingRepository(ApplicationClientConfiguration configuration) {
            this.configuration = configuration;
        }

        @Override
        public Optional<ApplicationClientConfiguration> reserveNext(
                UUID workerId,
                Instant now,
                Duration leaseDuration) {
            return Optional.ofNullable(configuration);
        }

        @Override
        public void markApplied(UUID operationId, UUID workerId, Instant now) {
            transition = "APPLIED";
        }

        @Override
        public void scheduleRetry(
                UUID operationId,
                UUID workerId,
                int attempts,
                Instant nextAttemptAt,
                String failureCode,
                Instant now) {
            transition = "RETRY";
            this.attempts = attempts;
            this.nextAttemptAt = nextAttemptAt;
            this.failureCode = failureCode;
        }

        @Override
        public void markFailed(
                UUID operationId,
                UUID workerId,
                int attempts,
                String failureCode,
                Instant now) {
            transition = "FAILED";
            this.attempts = attempts;
            this.failureCode = failureCode;
        }
    }
}
