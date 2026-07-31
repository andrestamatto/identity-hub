package br.dev.andrestamatto.identityhub.clientapplication.application;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

public final class ProcessApplicationClientProjection {

    private final ApplicationClientProjectionRepository repository;
    private final ApplicationClientProjector projector;
    private final Clock clock;
    private final Duration leaseDuration;
    private final Duration initialRetryDelay;
    private final int maxAttempts;

    public ProcessApplicationClientProjection(
            ApplicationClientProjectionRepository repository,
            ApplicationClientProjector projector,
            Clock clock,
            Duration leaseDuration,
            Duration initialRetryDelay,
            int maxAttempts) {
        this.repository = Objects.requireNonNull(repository);
        this.projector = Objects.requireNonNull(projector);
        this.clock = Objects.requireNonNull(clock);
        this.leaseDuration = requirePositive(leaseDuration, "Lease duration");
        this.initialRetryDelay = requirePositive(initialRetryDelay, "Initial retry delay");
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("Maximum attempts must be positive");
        }
        this.maxAttempts = maxAttempts;
    }

    public ApplicationClientProjectionResult processNext(UUID workerId) {
        Objects.requireNonNull(workerId);
        var now = clock.instant();
        var reserved = repository.reserveNext(workerId, now, leaseDuration);
        if (reserved.isEmpty()) {
            return ApplicationClientProjectionResult.NO_WORK;
        }

        var configuration = reserved.orElseThrow();
        var projection = configuration.projection();
        var attempts = projection.attempts() + 1;
        try {
            projector.project(ApplicationClientSnapshot.from(configuration));
            repository.markApplied(projection.operationId(), workerId, clock.instant());
            return ApplicationClientProjectionResult.APPLIED;
        } catch (ApplicationClientProjectionException exception) {
            if (!exception.retryable() || attempts >= maxAttempts) {
                repository.markFailed(
                        projection.operationId(),
                        workerId,
                        attempts,
                        exception.failureCode().name(),
                        clock.instant());
                return ApplicationClientProjectionResult.FAILED;
            }
            var retryAt = clock.instant().plus(retryDelay(attempts));
            repository.scheduleRetry(
                    projection.operationId(),
                    workerId,
                    attempts,
                    retryAt,
                    exception.failureCode().name(),
                    clock.instant());
            return ApplicationClientProjectionResult.RETRY_SCHEDULED;
        }
    }

    private Duration retryDelay(int attempts) {
        return initialRetryDelay.multipliedBy(1L << Math.min(attempts - 1, 20));
    }

    private static Duration requirePositive(Duration duration, String name) {
        Objects.requireNonNull(duration);
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return duration;
    }
}
