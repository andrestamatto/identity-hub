package br.dev.andrestamatto.identityhub.identity.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class MinimumPublicResponseTimingTest {

    @Test
    void waitsOnlyForTheRemainderOfTheMinimumDuration() {
        var nanoTime = new AtomicLong(1_000_000_000L);
        var waits = new ArrayList<Long>();
        var timing = new MinimumPublicResponseTiming(
                Duration.ofMillis(750), nanoTime::get, waits::add);

        var scope = timing.begin();
        nanoTime.addAndGet(Duration.ofMillis(200).toNanos());
        scope.close();

        assertThat(waits).containsExactly(Duration.ofMillis(550).toNanos());
    }

    @Test
    void doesNotWaitWhenProcessingAlreadyExceededTheMinimum() {
        var nanoTime = new AtomicLong(1_000_000_000L);
        var waits = new ArrayList<Long>();
        var timing = new MinimumPublicResponseTiming(
                Duration.ofMillis(750), nanoTime::get, waits::add);

        var scope = timing.begin();
        nanoTime.addAndGet(Duration.ofSeconds(1).toNanos());
        scope.close();

        assertThat(waits).isEmpty();
    }
}
