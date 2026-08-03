package br.dev.andrestamatto.identityhub.access.application;

import br.dev.andrestamatto.identityhub.access.domain.Membership;
import br.dev.andrestamatto.identityhub.access.domain.MembershipId;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface MembershipProjectionRepository {

    Optional<MembershipProjectionTask> reserveNext(
            UUID workerId, Instant now, Duration leaseDuration);

    void markApplied(Membership activeMembership, UUID workerId, Instant now);

    void scheduleRetry(
            MembershipId membershipId,
            UUID workerId,
            int attempts,
            Instant nextAttemptAt,
            String failureCode,
            Instant now);

    void markFailed(
            MembershipId membershipId,
            UUID workerId,
            int attempts,
            String failureCode,
            Instant now);
}
