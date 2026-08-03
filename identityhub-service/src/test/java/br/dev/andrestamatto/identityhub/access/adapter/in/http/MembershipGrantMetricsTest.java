package br.dev.andrestamatto.identityhub.access.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;

import br.dev.andrestamatto.identityhub.access.application.MembershipGrantResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MembershipGrantMetricsTest {

    @Test
    void recordsOnlyAStableOutcomeWithoutIdentifiers() {
        var registry = new SimpleMeterRegistry();
        var metrics = new MembershipGrantMetrics(registry);

        metrics.record(() -> new MembershipGrantResult(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PENDING",
                Instant.parse("2026-08-02T18:00:00Z")));

        var timer = registry.get(MembershipGrantMetrics.METRIC_NAME)
                .tag("outcome", "accepted")
                .timer();
        assertThat(timer.count()).isOne();
        assertThat(timer.getId().getTags())
                .extracting(tag -> tag.getKey())
                .containsExactly("outcome");
    }
}
