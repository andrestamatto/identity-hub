package br.dev.andrestamatto.identityhub.communication.application;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

public final class ProcessEmailDelivery {

    private final EmailDeliveryRepository repository;
    private final EmailDeliverySender sender;
    private final EmailDeliveryRenderer renderer;
    private final Clock clock;
    private final Duration leaseDuration;
    private final Duration initialRetryDelay;
    private final int maxAttempts;

    public ProcessEmailDelivery(
            EmailDeliveryRepository repository,
            EmailDeliverySender sender,
            EmailDeliveryRenderer renderer,
            Clock clock,
            Duration leaseDuration,
            Duration initialRetryDelay,
            int maxAttempts) {
        this.repository = Objects.requireNonNull(repository);
        this.sender = Objects.requireNonNull(sender);
        this.renderer = Objects.requireNonNull(renderer);
        this.clock = Objects.requireNonNull(clock);
        this.leaseDuration = requirePositive(leaseDuration, "Lease duration");
        this.initialRetryDelay = requirePositive(initialRetryDelay, "Initial retry delay");
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("Maximum attempts must be positive");
        }
        this.maxAttempts = maxAttempts;
    }

    public ProcessEmailDelivery(
            EmailDeliveryRepository repository,
            EmailDeliverySender sender,
            PasswordChangedEmailRenderer renderer,
            Clock clock,
            Duration leaseDuration,
            Duration initialRetryDelay,
            int maxAttempts) {
        this(
                repository,
                sender,
                new EmailDeliveryRenderer(
                        renderer,
                        new EmailVerificationEmailRenderer(),
                        new PasswordRecoveryEmailRenderer()),
                clock,
                leaseDuration,
                initialRetryDelay,
                maxAttempts);
    }

    public EmailDeliveryResult processNext(UUID workerId) {
        Objects.requireNonNull(workerId);
        var reserved = repository.reserveNext(workerId, clock.instant(), leaseDuration);
        if (reserved.isEmpty()) {
            return EmailDeliveryResult.NO_WORK;
        }
        var delivery = reserved.orElseThrow();
        var attempts = delivery.attempts() + 1;
        try {
            sender.send(renderer.render(delivery));
            repository.markDelivered(delivery.id(), workerId, clock.instant());
            return EmailDeliveryResult.DELIVERED;
        } catch (EmailDeliveryException exception) {
            if (!exception.retryable() || attempts >= maxAttempts) {
                repository.markFailed(
                        delivery.id(), workerId, attempts,
                        exception.failureCode().name(), clock.instant());
                return EmailDeliveryResult.FAILED;
            }
            repository.scheduleRetry(
                    delivery.id(), workerId, attempts,
                    clock.instant().plus(retryDelay(attempts)),
                    exception.failureCode().name(), clock.instant());
            return EmailDeliveryResult.RETRY_SCHEDULED;
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
