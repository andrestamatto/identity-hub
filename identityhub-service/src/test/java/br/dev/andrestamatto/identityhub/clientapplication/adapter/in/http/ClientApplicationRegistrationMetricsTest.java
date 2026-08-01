package br.dev.andrestamatto.identityhub.clientapplication.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationConflictException;
import br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationRegistration;
import br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationSnapshot;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplicationState;
import br.dev.andrestamatto.identityhub.clientapplication.domain.SelfRegistrationPolicy;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClientApplicationRegistrationMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final ClientApplicationRegistrationMetrics metrics =
            new ClientApplicationRegistrationMetrics(registry);

    @Test
    void recordsCreatedAndReplayedRegistrationsWithoutApplicationTags() {
        metrics.record(() -> registration(true));
        metrics.record(() -> registration(false));

        assertThat(timerCount("created")).isEqualTo(1);
        assertThat(timerCount("replayed")).isEqualTo(1);
        assertThat(registry.getMeters())
                .allSatisfy(meter -> assertThat(meter.getId().getTags())
                        .extracting("key")
                        .containsExactly("outcome"));
    }

    @Test
    void recordsConflictAndRethrowsIt() {
        assertThatThrownBy(() -> metrics.record(() -> {
                    throw new ClientApplicationConflictException("synthetic conflict");
                }))
                .isInstanceOf(ClientApplicationConflictException.class);

        assertThat(timerCount("conflict")).isEqualTo(1);
    }

    private ClientApplicationRegistration registration(boolean created) {
        return new ClientApplicationRegistration(
                new ClientApplicationSnapshot(
                        UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0"),
                        "auto-radar",
                        "Auto Radar",
                        ClientApplicationState.DRAFT,
                        SelfRegistrationPolicy.DISABLED,
                        Instant.parse("2026-07-30T14:00:00Z")),
                created);
    }

    private long timerCount(String outcome) {
        return registry.get(ClientApplicationRegistrationMetrics.METRIC_NAME)
                .tag("outcome", outcome)
                .timer()
                .count();
    }
}
