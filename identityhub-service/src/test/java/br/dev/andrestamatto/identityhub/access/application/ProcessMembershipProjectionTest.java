package br.dev.andrestamatto.identityhub.access.application;

import static org.assertj.core.api.Assertions.assertThat;

import br.dev.andrestamatto.identityhub.access.domain.Membership;
import br.dev.andrestamatto.identityhub.access.domain.MembershipApplicationRef;
import br.dev.andrestamatto.identityhub.access.domain.MembershipId;
import br.dev.andrestamatto.identityhub.access.domain.MembershipUserAccountRef;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProcessMembershipProjectionTest {

    private static final Instant NOW = Instant.parse("2026-08-03T01:00:00Z");
    private static final UUID WORKER =
            UUID.fromString("2e993831-1468-44f7-958d-b82a80a784bb");

    @Test
    void activatesOnlyAfterSuccessfulProjection() {
        var repository = new RecordingRepository(task(0));
        var processor = processor(repository, membership -> { });

        assertThat(processor.processNext(WORKER)).isEqualTo(MembershipProjectionResult.APPLIED);
        assertThat(repository.transition).isEqualTo("APPLIED");
        assertThat(repository.active.state().name()).isEqualTo("ACTIVE");
    }

    @Test
    void retriesTransientFailureWithBackoff() {
        var repository = new RecordingRepository(task(1));
        var processor = processor(repository, membership -> {
            throw MembershipProjectionException.retryable(
                    MembershipProjectionFailureCode.KEYCLOAK_UNAVAILABLE, null);
        });

        assertThat(processor.processNext(WORKER))
                .isEqualTo(MembershipProjectionResult.RETRY_SCHEDULED);
        assertThat(repository.transition).isEqualTo("RETRY");
        assertThat(repository.attempts).isEqualTo(2);
        assertThat(repository.nextAttemptAt).isEqualTo(NOW.plusSeconds(20));
    }

    @Test
    void keepsMembershipPendingOnPermanentFailure() {
        var repository = new RecordingRepository(task(0));
        var processor = processor(repository, membership -> {
            throw MembershipProjectionException.permanent(
                    MembershipProjectionFailureCode.USER_NOT_FOUND, null);
        });

        assertThat(processor.processNext(WORKER)).isEqualTo(MembershipProjectionResult.FAILED);
        assertThat(repository.transition).isEqualTo("FAILED");
        assertThat(repository.active).isNull();
    }

    private ProcessMembershipProjection processor(
            MembershipProjectionRepository repository,
            MembershipProjector projector) {
        return new ProcessMembershipProjection(
                repository,
                projector,
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofSeconds(30),
                Duration.ofSeconds(10),
                3);
    }

    private MembershipProjectionTask task(int attempts) {
        var membership = Membership.request(
                new MembershipId(UUID.fromString("c50638fe-0b91-4f47-81e6-2bd183040c1c")),
                new MembershipApplicationRef(
                        UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0")),
                new MembershipUserAccountRef(
                        UUID.fromString("680ac2e4-bfb0-4375-a75e-453b6e7b600c")),
                Clock.fixed(NOW.minusSeconds(60), ZoneOffset.UTC));
        return new MembershipProjectionTask(membership, attempts, "membership-projection");
    }

    private static final class RecordingRepository implements MembershipProjectionRepository {
        private final MembershipProjectionTask task;
        private String transition;
        private int attempts;
        private Instant nextAttemptAt;
        private Membership active;

        private RecordingRepository(MembershipProjectionTask task) {
            this.task = task;
        }

        @Override
        public Optional<MembershipProjectionTask> reserveNext(
                UUID workerId, Instant now, Duration leaseDuration) {
            return Optional.ofNullable(task);
        }

        @Override
        public void markApplied(Membership membership, UUID workerId, Instant now) {
            transition = "APPLIED";
            active = membership;
        }

        @Override
        public void scheduleRetry(
                MembershipId membershipId,
                UUID workerId,
                int attempts,
                Instant nextAttemptAt,
                String failureCode,
                Instant now) {
            transition = "RETRY";
            this.attempts = attempts;
            this.nextAttemptAt = nextAttemptAt;
        }

        @Override
        public void markFailed(
                MembershipId membershipId,
                UUID workerId,
                int attempts,
                String failureCode,
                Instant now) {
            transition = "FAILED";
        }
    }
}
