package br.dev.andrestamatto.identityhub.clientapplication.adapter.out.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientConfiguration;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientConfigurationRepository;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientProjection;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientProjectionState;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClient;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientKey;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationIdentifier;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplication;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplicationId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.DisplayName;
import br.dev.andrestamatto.identityhub.clientapplication.domain.MachineClientScope;
import br.dev.andrestamatto.identityhub.clientapplication.domain.MachineSettings;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class KeycloakMembershipProvisioningClientResolverTest {

    private static final UUID APPLICATION_ID =
            UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0");
    private static final UUID CLIENT_ID =
            UUID.fromString("72c43df3-9f34-4dc6-85cc-5d323762f299");
    private static final Instant NOW = Instant.parse("2026-08-02T18:00:00Z");

    private final ApplicationClientConfigurationRepository repository =
            org.mockito.Mockito.mock(ApplicationClientConfigurationRepository.class);
    private final KeycloakMembershipProvisioningClientResolver resolver =
            new KeycloakMembershipProvisioningClientResolver(repository);

    @Test
    void resolvesOnlyAnAppliedEnabledMachineClientWithExplicitPermission() {
        when(repository.findById(new ApplicationClientId(CLIENT_ID)))
                .thenReturn(Optional.of(configuration(true, true, true)));

        var resolved = resolver.resolve("ih-machine-" + CLIENT_ID).orElseThrow();

        assertThat(resolved.applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(resolved.applicationClientId()).isEqualTo(CLIENT_ID);
    }

    @Test
    void rejectsUnknownMalformedPendingDisabledOrUnprivilegedClients() {
        assertThat(resolver.resolve("external-client")).isEmpty();
        assertThat(resolver.resolve("ih-machine-not-a-uuid")).isEmpty();

        when(repository.findById(new ApplicationClientId(CLIENT_ID)))
                .thenReturn(Optional.of(configuration(false, true, true)));
        assertThat(resolver.resolve("ih-machine-" + CLIENT_ID)).isEmpty();

        when(repository.findById(new ApplicationClientId(CLIENT_ID)))
                .thenReturn(Optional.of(configuration(true, false, true)));
        assertThat(resolver.resolve("ih-machine-" + CLIENT_ID)).isEmpty();

        when(repository.findById(new ApplicationClientId(CLIENT_ID)))
                .thenReturn(Optional.of(configuration(true, true, false)));
        assertThat(resolver.resolve("ih-machine-" + CLIENT_ID)).isEmpty();
    }

    private ApplicationClientConfiguration configuration(
            boolean applied,
            boolean privileged,
            boolean enabled) {
        var clock = Clock.fixed(NOW, ZoneOffset.UTC);
        var application = ClientApplication.register(
                new ClientApplicationId(APPLICATION_ID),
                new ApplicationIdentifier("catalog"),
                new DisplayName("Catalog"),
                clock);
        var settings = new MachineSettings(privileged
                ? List.of(MachineClientScope.MEMBERSHIP_WRITE)
                : List.of());
        var configured = application.configureMachine(
                new ApplicationClientId(CLIENT_ID),
                new ApplicationClientKey("membership-provisioner"),
                settings,
                clock);
        var client = ApplicationClient.reconstitute(
                        configured.id(),
                        configured.applicationId(),
                        configured.key(),
                        configured.settings(),
                        enabled,
                        configured.configuredAt());
        var projection = ApplicationClientProjection.pending(
                UUID.fromString("06d068e0-78d5-4df6-97dd-88df538ab948"),
                client.id(),
                "resolve-provisioner",
                NOW);
        if (applied) {
            projection = new ApplicationClientProjection(
                    projection.operationId(),
                    projection.clientId(),
                    projection.payloadVersion(),
                    projection.correlationId(),
                    ApplicationClientProjectionState.APPLIED,
                    1,
                    projection.nextAttemptAt(),
                    null,
                    projection.createdAt(),
                    NOW);
        }
        return new ApplicationClientConfiguration(client, projection);
    }
}
