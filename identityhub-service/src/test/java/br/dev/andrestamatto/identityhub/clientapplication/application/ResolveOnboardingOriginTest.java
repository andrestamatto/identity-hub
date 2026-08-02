package br.dev.andrestamatto.identityhub.clientapplication.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientKey;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationIdentifier;
import br.dev.andrestamatto.identityhub.clientapplication.domain.BrowserTransportPolicy;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplication;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplicationId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.DisplayName;
import br.dev.andrestamatto.identityhub.clientapplication.domain.MachineSettings;
import br.dev.andrestamatto.identityhub.clientapplication.domain.SpaSettings;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResolveOnboardingOriginTest {

    private static final UUID APPLICATION_ID = UUID.randomUUID();
    private static final UUID MACHINE_CLIENT_ID = UUID.randomUUID();
    private static final UUID BROWSER_CLIENT_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-08-01T20:00:00Z");
    private static final String REDIRECT_URI = "https://app.example.com/auth/callback";

    private final ApplicationClientConfigurationRepository repository =
            org.mockito.Mockito.mock(ApplicationClientConfigurationRepository.class);
    private final ResolveOnboardingOrigin resolve = new ResolveOnboardingOrigin(repository);

    @Test
    void resolvesOnlyProjectedClientsFromTheSameApplication() {
        when(repository.findById(new ApplicationClientId(MACHINE_CLIENT_ID)))
                .thenReturn(Optional.of(machineConfiguration()));
        when(repository.findById(new ApplicationClientId(BROWSER_CLIENT_ID)))
                .thenReturn(Optional.of(browserConfiguration(APPLICATION_ID)));

        var result = resolve.execute(MACHINE_CLIENT_ID, BROWSER_CLIENT_ID, REDIRECT_URI);

        assertThat(result.applicationId()).isEqualTo(APPLICATION_ID);
    }

    @Test
    void rejectsCrossApplicationRedirectAndMissingMachineScope() {
        when(repository.findById(new ApplicationClientId(MACHINE_CLIENT_ID)))
                .thenReturn(Optional.of(machineConfiguration()));
        when(repository.findById(new ApplicationClientId(BROWSER_CLIENT_ID)))
                .thenReturn(Optional.of(browserConfiguration(UUID.randomUUID())));

        assertThatThrownBy(() -> resolve.execute(
                        MACHINE_CLIENT_ID, BROWSER_CLIENT_ID, REDIRECT_URI))
                .isInstanceOf(OnboardingOriginRejectedException.class);

        when(repository.findById(new ApplicationClientId(MACHINE_CLIENT_ID)))
                .thenReturn(Optional.of(machineConfigurationWithoutScope()));
        when(repository.findById(new ApplicationClientId(BROWSER_CLIENT_ID)))
                .thenReturn(Optional.of(browserConfiguration(APPLICATION_ID)));
        assertThatThrownBy(() -> resolve.execute(
                        MACHINE_CLIENT_ID, BROWSER_CLIENT_ID, REDIRECT_URI))
                .isInstanceOf(OnboardingOriginRejectedException.class);

        when(repository.findById(new ApplicationClientId(MACHINE_CLIENT_ID)))
                .thenReturn(Optional.of(machineConfiguration()));
        assertThatThrownBy(() -> resolve.execute(
                        MACHINE_CLIENT_ID, BROWSER_CLIENT_ID,
                        "https://attacker.example/callback"))
                .isInstanceOf(OnboardingOriginRejectedException.class);
    }

    private ApplicationClientConfiguration machineConfiguration() {
        return machineConfiguration(MachineSettings.create(List.of("onboarding:write")));
    }

    private ApplicationClientConfiguration machineConfigurationWithoutScope() {
        return machineConfiguration(new MachineSettings());
    }

    private ApplicationClientConfiguration machineConfiguration(MachineSettings settings) {
        var client = application(APPLICATION_ID).configureMachine(
                new ApplicationClientId(MACHINE_CLIENT_ID),
                new ApplicationClientKey("onboarding-machine"),
                settings,
                clock());
        return applied(client);
    }

    private ApplicationClientConfiguration browserConfiguration(UUID applicationId) {
        var client = application(applicationId).configureSpa(
                new ApplicationClientId(BROWSER_CLIENT_ID),
                new ApplicationClientKey("application-web"),
                SpaSettings.create(
                        List.of(REDIRECT_URI),
                        List.of("https://app.example.com"),
                        BrowserTransportPolicy.PRODUCTION),
                clock());
        return applied(client);
    }

    private ApplicationClientConfiguration applied(
            br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClient client) {
        return new ApplicationClientConfiguration(
                client,
                new ApplicationClientProjection(
                        UUID.randomUUID(),
                        client.id(),
                        1,
                        "onboarding-origin-test",
                        ApplicationClientProjectionState.APPLIED,
                        1,
                        NOW,
                        null,
                        NOW,
                        NOW));
    }

    private ClientApplication application(UUID applicationId) {
        return ClientApplication.register(
                new ClientApplicationId(applicationId),
                new ApplicationIdentifier("app-" + applicationId.toString().substring(0, 8)),
                new DisplayName("Application"),
                clock());
    }

    private Clock clock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }
}
