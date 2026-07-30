package br.dev.andrestamatto.identityhub.audit.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AdministrativeAccessAuditTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-07-29T12:00:00Z");
    private static final UUID EVENT_ID = UUID.fromString("d00e951b-adf7-4ba3-8358-a31e69999266");

    @Test
    void recordsOnlyTheMinimumAdministrativeAccessEvidence() {
        var repository = new CapturingRepository();
        var audit = new AdministrativeAccessAudit(
                repository,
                Clock.fixed(OCCURRED_AT, ZoneOffset.UTC),
                () -> EVENT_ID);

        audit.record(new AdministrativeAccessAttempt(
                "correlation-123",
                "operator-id",
                "GET",
                "/internal/admin/runtime",
                AdministrativeAccessOutcome.ALLOWED,
                "authorized"));

        assertThat(repository.events).singleElement().satisfies(event -> {
            assertThat(event.id()).isEqualTo(EVENT_ID);
            assertThat(event.occurredAt()).isEqualTo(OCCURRED_AT);
            assertThat(event.correlationId()).isEqualTo("correlation-123");
            assertThat(event.actorSubject()).isEqualTo("operator-id");
            assertThat(event.method()).isEqualTo("GET");
            assertThat(event.path()).isEqualTo("/internal/admin/runtime");
            assertThat(event.outcome()).isEqualTo(AdministrativeAccessOutcome.ALLOWED);
            assertThat(event.reason()).isEqualTo("authorized");
        });
    }

    private static final class CapturingRepository implements AdministrativeAccessEventRepository {

        private final List<AdministrativeAccessEvent> events = new ArrayList<>();

        @Override
        public void append(AdministrativeAccessEvent event) {
            events.add(event);
        }
    }
}
