package br.dev.andrestamatto.identityhub.identity.adapter.in.http;

import java.time.Duration;
import java.util.Objects;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

public final class MinimumPublicResponseTiming implements PublicResponseTiming {

    private final long minimumNanos;
    private final LongSupplier nanoTime;
    private final LongConsumer waitNanos;

    public MinimumPublicResponseTiming(
            Duration minimumDuration,
            LongSupplier nanoTime,
            LongConsumer waitNanos) {
        Objects.requireNonNull(minimumDuration);
        if (minimumDuration.isNegative()) {
            throw new IllegalArgumentException("Minimum public response time cannot be negative");
        }
        this.minimumNanos = minimumDuration.toNanos();
        this.nanoTime = Objects.requireNonNull(nanoTime);
        this.waitNanos = Objects.requireNonNull(waitNanos);
    }

    @Override
    public Scope begin() {
        var startedAt = nanoTime.getAsLong();
        return () -> {
            var remaining = minimumNanos - (nanoTime.getAsLong() - startedAt);
            if (remaining > 0) {
                waitNanos.accept(remaining);
            }
        };
    }
}
