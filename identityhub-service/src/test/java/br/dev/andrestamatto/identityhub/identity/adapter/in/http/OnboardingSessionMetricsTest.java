package br.dev.andrestamatto.identityhub.identity.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.dev.andrestamatto.identityhub.identity.application.BeginOnboardingSession;
import br.dev.andrestamatto.identityhub.identity.application.OnboardingSessionConflictException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OnboardingSessionMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final OnboardingSessionMetrics metrics = new OnboardingSessionMetrics(registry);

    @Test
    void recordsOnlyBoundedOutcomesWithoutSensitiveTags() {
        metrics.record(() -> result(true));
        metrics.record(() -> result(false));

        assertThat(timerCount("created")).isOne();
        assertThat(timerCount("replayed")).isOne();
        assertThat(registry.getMeters())
                .allSatisfy(meter -> assertThat(meter.getId().getTags())
                        .extracting("key")
                        .containsExactly("outcome"));
    }

    @Test
    void recordsConflictAndPreservesFailure() {
        assertThatThrownBy(() -> metrics.record(() -> {
                    throw new OnboardingSessionConflictException();
                }))
                .isInstanceOf(OnboardingSessionConflictException.class);

        assertThat(timerCount("conflict")).isOne();
    }

    private BeginOnboardingSession.Result result(boolean created) {
        return new BeginOnboardingSession.Result(
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                Instant.parse("2026-08-01T20:10:00Z"),
                created);
    }

    private long timerCount(String outcome) {
        return registry.get(OnboardingSessionMetrics.METRIC_NAME)
                .tag("outcome", outcome)
                .timer()
                .count();
    }
}
