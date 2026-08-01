package br.dev.andrestamatto.identityhub.identity.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class InMemoryRegistrationRateLimiterTest {

    private final TestClock clock = new TestClock(Instant.parse("2026-08-01T10:00:00Z"));

    @Test
    void limitsEachRemoteAddressWithinItsOwnWindow() {
        var limiter = new InMemoryRegistrationRateLimiter(
                2, Duration.ofMinutes(15), 100, clock);

        limiter.acquire("192.0.2.10");
        limiter.acquire("192.0.2.10");

        assertThatThrownBy(() -> limiter.acquire("192.0.2.10"))
                .isInstanceOf(PublicRegistrationRateLimitException.class)
                .satisfies(exception -> assertThat(
                                ((PublicRegistrationRateLimitException) exception)
                                        .retryAfterSeconds())
                        .isEqualTo(900));
        limiter.acquire("192.0.2.11");
    }

    @Test
    void opensANewWindowAfterTheConfiguredDuration() {
        var limiter = new InMemoryRegistrationRateLimiter(
                1, Duration.ofMinutes(15), 100, clock);
        limiter.acquire("192.0.2.10");

        clock.advance(Duration.ofMinutes(15));

        limiter.acquire("192.0.2.10");
    }

    @Test
    void failsClosedWhenTheBoundedSourceTableIsFull() {
        var limiter = new InMemoryRegistrationRateLimiter(
                2, Duration.ofMinutes(15), 1, clock);
        limiter.acquire("192.0.2.10");

        assertThatThrownBy(() -> limiter.acquire("192.0.2.11"))
                .isInstanceOf(PublicRegistrationRateLimitException.class);
    }

    private static final class TestClock extends Clock {
        private Instant current;

        private TestClock(Instant current) {
            this.current = current;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }

        private void advance(Duration duration) {
            current = current.plus(duration);
        }
    }
}
