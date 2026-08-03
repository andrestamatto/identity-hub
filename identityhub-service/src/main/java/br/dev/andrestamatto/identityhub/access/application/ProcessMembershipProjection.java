package br.dev.andrestamatto.identityhub.access.application;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

public final class ProcessMembershipProjection {

    private final MembershipProjectionRepository repository;
    private final MembershipProjector projector;
    private final Clock clock;
    private final Duration leaseDuration;
    private final Duration initialRetryDelay;
    private final int maxAttempts;

    public ProcessMembershipProjection(
            MembershipProjectionRepository repository,
            MembershipProjector projector,
            Clock clock,
            Duration leaseDuration,
            Duration initialRetryDelay,
            int maxAttempts) {
        this.repository = Objects.requireNonNull(repository);
        this.projector = Objects.requireNonNull(projector);
        this.clock = Objects.requireNonNull(clock);
        this.leaseDuration = positive(leaseDuration, "Lease duration");
        this.initialRetryDelay = positive(initialRetryDelay, "Initial retry delay");
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("Maximum attempts must be positive");
        }
        this.maxAttempts = maxAttempts;
    }

    public MembershipProjectionResult processNext(UUID workerId) {
        Objects.requireNonNull(workerId);
        var reserved = repository.reserveNext(workerId, clock.instant(), leaseDuration);
        if (reserved.isEmpty()) {
            return MembershipProjectionResult.NO_WORK;
        }
        var task = reserved.orElseThrow();
        var attempts = task.attempts() + 1;
        try {
            projector.project(task.membership());
            repository.markApplied(
                    task.membership().activate(clock), workerId, clock.instant());
            return MembershipProjectionResult.APPLIED;
        } catch (MembershipProjectionException exception) {
            if (!exception.retryable() || attempts >= maxAttempts) {
                repository.markFailed(
                        task.membership().id(),
                        workerId,
                        attempts,
                        exception.failureCode().name(),
                        clock.instant());
                return MembershipProjectionResult.FAILED;
            }
            repository.scheduleRetry(
                    task.membership().id(),
                    workerId,
                    attempts,
                    clock.instant().plus(retryDelay(attempts)),
                    exception.failureCode().name(),
                    clock.instant());
            return MembershipProjectionResult.RETRY_SCHEDULED;
        }
    }

    private Duration retryDelay(int attempts) {
        return initialRetryDelay.multipliedBy(1L << Math.min(attempts - 1, 20));
    }

    private static Duration positive(Duration duration, String name) {
        Objects.requireNonNull(duration);
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return duration;
    }
}
