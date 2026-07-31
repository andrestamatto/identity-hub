package br.dev.andrestamatto.identityhub.clientapplication.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationIdentifier;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientKey;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplication;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplicationId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.DisplayName;
import br.dev.andrestamatto.identityhub.clientapplication.domain.SpaSettings;
import br.dev.andrestamatto.identityhub.clientapplication.domain.SpaTransportPolicy;
import br.dev.andrestamatto.identityhub.clientapplication.domain.TokenAudience;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfigureSpaClientTest {

    private static final UUID APPLICATION_ID =
            UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0");
    private static final UUID CLIENT_ID =
            UUID.fromString("ff7c4748-f053-4fb6-91be-d34cf0015834");
    private static final UUID OPERATION_ID =
            UUID.fromString("27f3aa0b-6a70-43bd-a087-d5bc0c1bc779");
    private static final Instant NOW = Instant.parse("2026-08-01T12:00:00Z");

    private final ClientApplicationRepository applicationRepository =
            org.mockito.Mockito.mock(ClientApplicationRepository.class);
    private final ApplicationClientConfigurationRepository clientRepository =
            org.mockito.Mockito.mock(ApplicationClientConfigurationRepository.class);
    private final ConfigureSpaClient configure = new ConfigureSpaClient(
            applicationRepository,
            clientRepository,
            SpaTransportPolicy.DEVELOPMENT,
            Clock.fixed(NOW, ZoneOffset.UTC),
            () -> OPERATION_ID);

    @BeforeEach
    void applicationExists() {
        when(applicationRepository.findById(any())).thenReturn(Optional.of(application()));
        when(clientRepository.findById(any())).thenReturn(Optional.empty());
        when(clientRepository.findByKey(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void configuresPublicSpaWithPendingProjection() {
        var result = configure.execute(command());

        assertThat(result.created()).isTrue();
        assertThat(result.client().type()).isEqualTo("SPA");
        assertThat(result.client().audience()).isNull();
        assertThat(result.client().redirectUris())
                .containsExactly("http://127.0.0.1:5173/auth/callback");
        assertThat(result.client().webOrigins())
                .containsExactly("http://127.0.0.1:5173");
        assertThat(result.client().projectionState())
                .isEqualTo(ApplicationClientProjectionState.PENDING);
        verify(clientRepository).add(any(ApplicationClientConfiguration.class));
    }

    @Test
    void identicalRetryReturnsExistingConfiguration() {
        var first = configure.execute(command());
        var stored = new ApplicationClientConfiguration(
                application().configureSpa(
                        new ApplicationClientId(CLIENT_ID),
                        new ApplicationClientKey("social-catalog-web"),
                        SpaSettings.create(
                                command().redirectUris(),
                                command().webOrigins(),
                                SpaTransportPolicy.DEVELOPMENT),
                        Clock.fixed(NOW, ZoneOffset.UTC)),
                ApplicationClientProjection.pending(
                        OPERATION_ID,
                        new ApplicationClientId(CLIENT_ID),
                        "configure-spa",
                        NOW));
        when(clientRepository.findById(any())).thenReturn(Optional.of(stored));

        var replay = configure.execute(command());

        assertThat(replay.created()).isFalse();
        assertThat(replay.client()).isEqualTo(first.client());
    }

    @Test
    void rejectsExistingClientIdWithDifferentTypeOrContent() {
        configure.execute(command());
        when(clientRepository.findById(any())).thenReturn(Optional.of(
                new ApplicationClientConfiguration(
                        application().configureProtectedApi(
                                new ApplicationClientId(CLIENT_ID),
                                new ApplicationClientKey("social-catalog-web"),
                                new TokenAudience("catalog-api"),
                                Clock.fixed(NOW, ZoneOffset.UTC)),
                        ApplicationClientProjection.pending(
                                OPERATION_ID,
                                new ApplicationClientId(CLIENT_ID),
                                "configure-api",
                                NOW))));

        assertThatThrownBy(() -> configure.execute(command()))
                .isInstanceOf(ClientApplicationConflictException.class);
        verify(clientRepository).add(any());
    }

    @Test
    void rejectsUnsafeTransportBeforePersistence() {
        var production = new ConfigureSpaClient(
                applicationRepository,
                clientRepository,
                SpaTransportPolicy.PRODUCTION,
                Clock.fixed(NOW, ZoneOffset.UTC),
                () -> OPERATION_ID);

        assertThatThrownBy(() -> production.execute(command()))
                .isInstanceOf(IllegalArgumentException.class);
        verify(clientRepository, never()).add(any());
    }

    private ConfigureSpaClient.Command command() {
        return new ConfigureSpaClient.Command(
                APPLICATION_ID,
                CLIENT_ID,
                "social-catalog-web",
                List.of("http://127.0.0.1:5173/auth/callback"),
                List.of("http://127.0.0.1:5173"),
                "configure-spa");
    }

    private ClientApplication application() {
        return ClientApplication.register(
                new ClientApplicationId(APPLICATION_ID),
                new ApplicationIdentifier("social-catalog"),
                new DisplayName("Social Catalog"),
                Clock.fixed(Instant.parse("2026-07-31T12:00:00Z"), ZoneOffset.UTC));
    }
}
