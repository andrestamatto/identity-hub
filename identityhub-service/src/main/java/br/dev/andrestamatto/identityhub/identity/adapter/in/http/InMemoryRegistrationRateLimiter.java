package br.dev.andrestamatto.identityhub.identity.adapter.in.http;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class InMemoryRegistrationRateLimiter {

    private final int requestLimit;
    private final Duration window;
    private final int trackedSourceLimit;
    private final Clock clock;
    private final Map<String, Window> windows = new HashMap<>();

    public InMemoryRegistrationRateLimiter(
            int requestLimit,
            Duration window,
            int trackedSourceLimit,
            Clock clock) {
        if (requestLimit < 1 || window.isZero() || window.isNegative()
                || trackedSourceLimit < 1) {
            throw new IllegalArgumentException("Registration rate limit must be positive");
        }
        this.requestLimit = requestLimit;
        this.window = window;
        this.trackedSourceLimit = trackedSourceLimit;
        this.clock = Objects.requireNonNull(clock);
    }

    synchronized void acquire(String remoteAddress) {
        Objects.requireNonNull(remoteAddress);
        var now = clock.instant();
        var current = windows.get(remoteAddress);
        if (current == null || !now.isBefore(current.expiresAt())) {
            purgeExpired(now);
            if (!windows.containsKey(remoteAddress) && windows.size() >= trackedSourceLimit) {
                throw new PublicRegistrationRateLimitException(window.toSeconds());
            }
            windows.put(remoteAddress, new Window(1, now.plus(window)));
            return;
        }
        if (current.requestCount() >= requestLimit) {
            var remainingMillis = Duration.between(now, current.expiresAt()).toMillis();
            throw new PublicRegistrationRateLimitException(
                    Math.max(1, (remainingMillis + 999) / 1_000));
        }
        windows.put(remoteAddress, new Window(
                current.requestCount() + 1, current.expiresAt()));
    }

    private void purgeExpired(Instant now) {
        windows.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt()));
    }

    private record Window(int requestCount, Instant expiresAt) { }
}
