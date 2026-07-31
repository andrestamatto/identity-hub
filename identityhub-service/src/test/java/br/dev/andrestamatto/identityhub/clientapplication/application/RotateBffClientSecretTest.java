package br.dev.andrestamatto.identityhub.clientapplication.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientKey;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationIdentifier;
import br.dev.andrestamatto.identityhub.clientapplication.domain.BffSettings;
import br.dev.andrestamatto.identityhub.clientapplication.domain.BrowserTransportPolicy;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplication;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplicationId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.DisplayName;
import br.dev.andrestamatto.identityhub.clientapplication.domain.TokenAudience;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RotateBffClientSecretTest {

    private static final UUID APPLICATION_ID =
            UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0");
    private static final UUID CLIENT_ID =
            UUID.fromString("72c43df3-9f34-4dc6-85cc-5d323762f299");
    private static final Instant NOW = Instant.parse("2026-08-01T14:00:00Z");

    private final ApplicationClientConfigurationRepository repository =
            org.mockito.Mockito.mock(ApplicationClientConfigurationRepository.class);
    private final BffClientSecretRotator rotator =
            org.mockito.Mockito.mock(BffClientSecretRotator.class);
    private final RotateBffClientSecret rotate = new RotateBffClientSecret(repository, rotator);

    @Test
    void rotatesSecretOnlyForAppliedBff() {
        var configuration = configuration(ApplicationClientProjectionState.APPLIED);
        when(repository.findById(any())).thenReturn(Optional.of(configuration));
        when(rotator.rotate(any())).thenReturn(new ConfidentialClientSecret("one-time-secret"));

        var secret = rotate.execute(APPLICATION_ID, CLIENT_ID);

        assertThat(secret.value()).isEqualTo("one-time-secret");
        assertThat(secret.toString()).doesNotContain("one-time-secret");
        verify(rotator).rotate(ApplicationClientSnapshot.from(configuration));
    }

    @Test
    void rejectsPendingProjectionWithoutCallingKeycloak() {
        when(repository.findById(any()))
                .thenReturn(Optional.of(configuration(ApplicationClientProjectionState.PENDING)));

        assertThatThrownBy(() -> rotate.execute(APPLICATION_ID, CLIENT_ID))
                .isInstanceOf(ClientApplicationConflictException.class);
        verify(rotator, never()).rotate(any());
    }

    @Test
    void hidesClientBelongingToAnotherApplication() {
        when(repository.findById(any()))
                .thenReturn(Optional.of(configuration(ApplicationClientProjectionState.APPLIED)));

        assertThatThrownBy(() -> rotate.execute(UUID.randomUUID(), CLIENT_ID))
                .isInstanceOf(ApplicationClientNotFoundException.class);
        verify(rotator, never()).rotate(any());
    }

    @Test
    void rejectsSecretRotationForNonBffClient() {
        when(repository.findById(any())).thenReturn(Optional.of(apiConfiguration()));

        assertThatThrownBy(() -> rotate.execute(APPLICATION_ID, CLIENT_ID))
                .isInstanceOf(IllegalArgumentException.class);
        verify(rotator, never()).rotate(any());
    }

    private ApplicationClientConfiguration configuration(
            ApplicationClientProjectionState state) {
        var clientId = new ApplicationClientId(CLIENT_ID);
        var client = application().configureBff(
                clientId,
                new ApplicationClientKey("social-catalog-bff"),
                BffSettings.create(
                        List.of("https://app.example.com/login/oauth2/code/identityhub"),
                        BrowserTransportPolicy.PRODUCTION),
                Clock.fixed(NOW, ZoneOffset.UTC));
        var projection = new ApplicationClientProjection(
                UUID.fromString("92390c62-b1f7-48d4-887a-d004a47faf8b"),
                clientId,
                1,
                "rotate-bff-secret",
                state,
                state == ApplicationClientProjectionState.APPLIED ? 1 : 0,
                NOW,
                null,
                NOW,
                NOW);
        return new ApplicationClientConfiguration(client, projection);
    }

    private ClientApplication application() {
        return ClientApplication.register(
                new ClientApplicationId(APPLICATION_ID),
                new ApplicationIdentifier("social-catalog"),
                new DisplayName("Social Catalog"),
                Clock.fixed(NOW.minusSeconds(3600), ZoneOffset.UTC));
    }

    private ApplicationClientConfiguration apiConfiguration() {
        var clientId = new ApplicationClientId(CLIENT_ID);
        var client = application().configureProtectedApi(
                clientId,
                new ApplicationClientKey("social-catalog-api"),
                new TokenAudience("social-catalog-api"),
                Clock.fixed(NOW, ZoneOffset.UTC));
        return new ApplicationClientConfiguration(
                client,
                new ApplicationClientProjection(
                        UUID.fromString("92390c62-b1f7-48d4-887a-d004a47faf8b"),
                        clientId,
                        1,
                        "rotate-api-secret",
                        ApplicationClientProjectionState.APPLIED,
                        1,
                        NOW,
                        null,
                        NOW,
                        NOW));
    }
}
