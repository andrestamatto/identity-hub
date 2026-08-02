package br.dev.andrestamatto.identityhub.clientapplication.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientKey;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationIdentifier;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplication;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplicationId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.DisplayName;
import br.dev.andrestamatto.identityhub.clientapplication.domain.MachineSettings;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfigureMachineClientTest {

    private static final UUID APPLICATION_ID =
            UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0");
    private static final UUID CLIENT_ID =
            UUID.fromString("72c43df3-9f34-4dc6-85cc-5d323762f299");
    private static final UUID OPERATION_ID =
            UUID.fromString("92390c62-b1f7-48d4-887a-d004a47faf8b");
    private static final Instant NOW = Instant.parse("2026-08-01T14:00:00Z");

    private final ClientApplicationRepository applicationRepository =
            org.mockito.Mockito.mock(ClientApplicationRepository.class);
    private final ApplicationClientConfigurationRepository clientRepository =
            org.mockito.Mockito.mock(ApplicationClientConfigurationRepository.class);
    private final ConfigureMachineClient configure = new ConfigureMachineClient(
            applicationRepository,
            clientRepository,
            Clock.fixed(NOW, ZoneOffset.UTC),
            () -> OPERATION_ID);

    @BeforeEach
    void applicationExists() {
        when(applicationRepository.findById(any())).thenReturn(Optional.of(application()));
        when(clientRepository.findById(any())).thenReturn(Optional.empty());
        when(clientRepository.findByKey(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void configuresMachineClientWithPendingProjection() {
        var result = configure.execute(command());

        assertThat(result.created()).isTrue();
        assertThat(result.client().type()).isEqualTo("MACHINE");
        assertThat(result.client().audience()).isNull();
        assertThat(result.client().redirectUris()).isEmpty();
        assertThat(result.client().webOrigins()).isEmpty();
        assertThat(result.client().projectionState())
                .isEqualTo(ApplicationClientProjectionState.PENDING);
        verify(clientRepository).add(any(ApplicationClientConfiguration.class));
    }

    @Test
    void identicalRetryReturnsExistingConfiguration() {
        var clientId = new ApplicationClientId(CLIENT_ID);
        var stored = new ApplicationClientConfiguration(
                application().configureMachine(
                        clientId,
                        new ApplicationClientKey("social-catalog-membership-provisioner"),
                        Clock.fixed(NOW, ZoneOffset.UTC)),
                ApplicationClientProjection.pending(
                        OPERATION_ID,
                        clientId,
                        "configure-machine",
                        NOW));
        when(clientRepository.findById(any())).thenReturn(Optional.of(stored));

        var replay = configure.execute(command());

        assertThat(replay.created()).isFalse();
        assertThat(replay.client().type()).isEqualTo(new MachineSettings().type().name());
        verify(clientRepository, never()).add(any());
    }

    private ConfigureMachineClient.Command command() {
        return new ConfigureMachineClient.Command(
                APPLICATION_ID,
                CLIENT_ID,
                "social-catalog-membership-provisioner",
                "configure-machine");
    }

    private ClientApplication application() {
        return ClientApplication.register(
                new ClientApplicationId(APPLICATION_ID),
                new ApplicationIdentifier("social-catalog"),
                new DisplayName("Social Catalog"),
                Clock.fixed(NOW.minusSeconds(3600), ZoneOffset.UTC));
    }
}
