package br.dev.andrestamatto.identityhub.clientapplication.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApplicationClientTest {

    private static final ClientApplicationId APPLICATION_ID = new ClientApplicationId(
            UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0"));
    private static final ApplicationClientId CLIENT_ID = new ApplicationClientId(
            UUID.fromString("ff7c4748-f053-4fb6-91be-d34cf0015834"));

    @Test
    void clientApplicationConfiguresEnabledProtectedApi() {
        var application = ClientApplication.reconstitute(
                APPLICATION_ID,
                new ApplicationIdentifier("social-catalog"),
                new DisplayName("Social Catalog"),
                ClientApplicationState.DRAFT,
                Instant.parse("2026-07-31T12:00:00Z"));
        var clock = Clock.fixed(
                Instant.parse("2026-07-31T14:00:00.123456789Z"),
                ZoneOffset.UTC);

        var client = application.configureProtectedApi(
                CLIENT_ID,
                new ApplicationClientKey("social-catalog-api"),
                new TokenAudience("social-catalog-api"),
                clock);

        assertThat(client.id()).isEqualTo(CLIENT_ID);
        assertThat(client.applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(client.key()).isEqualTo(new ApplicationClientKey("social-catalog-api"));
        assertThat(client.type()).isEqualTo(ApplicationClientType.API);
        assertThat(client.settings())
                .isEqualTo(new ProtectedApiSettings(new TokenAudience("social-catalog-api")));
        assertThat(client.enabled()).isTrue();
        assertThat(client.configuredAt())
                .isEqualTo(Instant.parse("2026-07-31T14:00:00.123456Z"));
    }
}
