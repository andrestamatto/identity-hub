package br.dev.andrestamatto.identityhub.access.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MembershipTest {

    @Test
    void requestsAccessWithoutActivatingItPrematurely() {
        var now = Instant.parse("2026-08-02T18:00:00Z");

        var membership = Membership.request(
                new MembershipId(UUID.fromString("c50638fe-0b91-4f47-81e6-2bd183040c1c")),
                new MembershipApplicationRef(
                        UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0")),
                new MembershipUserAccountRef(
                        UUID.fromString("680ac2e4-bfb0-4375-a75e-453b6e7b600c")),
                Clock.fixed(now, ZoneOffset.UTC));

        assertThat(membership.state()).isEqualTo(MembershipState.PENDING);
        assertThat(membership.requestedAt()).isEqualTo(now);
        assertThat(membership.activatedAt()).isNull();
    }

    @Test
    void activatesOnlyAfterTheProjectionIsConfirmed() {
        var requestedAt = Instant.parse("2026-08-02T18:00:00Z");
        var activatedAt = Instant.parse("2026-08-03T01:00:00Z");
        var membership = Membership.request(
                new MembershipId(UUID.fromString("c50638fe-0b91-4f47-81e6-2bd183040c1c")),
                new MembershipApplicationRef(
                        UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0")),
                new MembershipUserAccountRef(
                        UUID.fromString("680ac2e4-bfb0-4375-a75e-453b6e7b600c")),
                Clock.fixed(requestedAt, ZoneOffset.UTC));

        var active = membership.activate(Clock.fixed(activatedAt, ZoneOffset.UTC));

        assertThat(active.state()).isEqualTo(MembershipState.ACTIVE);
        assertThat(active.activatedAt()).isEqualTo(activatedAt);
        assertThat(active.activate(Clock.fixed(activatedAt.plusSeconds(1), ZoneOffset.UTC)))
                .isSameAs(active);
    }
}
