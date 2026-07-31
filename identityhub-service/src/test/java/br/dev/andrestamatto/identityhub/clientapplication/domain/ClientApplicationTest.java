package br.dev.andrestamatto.identityhub.clientapplication.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClientApplicationTest {

    private static final UUID APPLICATION_ID =
            UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0");
    private static final Instant REGISTERED_AT =
            Instant.parse("2026-07-30T14:00:00Z");

    @Test
    void registersIsolatedDraftApplication() {
        var application = ClientApplication.register(
                new ClientApplicationId(APPLICATION_ID),
                new ApplicationIdentifier("auto-radar"),
                new DisplayName("Auto Radar"),
                Clock.fixed(REGISTERED_AT, ZoneOffset.UTC));

        assertThat(application.id()).isEqualTo(new ClientApplicationId(APPLICATION_ID));
        assertThat(application.identifier()).isEqualTo(new ApplicationIdentifier("auto-radar"));
        assertThat(application.displayName()).isEqualTo(new DisplayName("Auto Radar"));
        assertThat(application.state()).isEqualTo(ClientApplicationState.DRAFT);
        assertThat(application.registeredAt()).isEqualTo(REGISTERED_AT);
    }

    @Test
    void normalizesRegistrationTimestampToMicroseconds() {
        var preciseInstant = Instant.parse("2026-07-30T14:00:00.123456789Z");

        var application = ClientApplication.register(
                new ClientApplicationId(APPLICATION_ID),
                new ApplicationIdentifier("auto-radar"),
                new DisplayName("Auto Radar"),
                Clock.fixed(preciseInstant, ZoneOffset.UTC));

        assertThat(application.registeredAt())
                .isEqualTo(Instant.parse("2026-07-30T14:00:00.123456Z"));
    }
}
