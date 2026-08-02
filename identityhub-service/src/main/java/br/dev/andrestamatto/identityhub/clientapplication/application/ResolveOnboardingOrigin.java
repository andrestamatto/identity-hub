package br.dev.andrestamatto.identityhub.clientapplication.application;

import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClient;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.BffSettings;
import br.dev.andrestamatto.identityhub.clientapplication.domain.MachineScope;
import br.dev.andrestamatto.identityhub.clientapplication.domain.MachineSettings;
import br.dev.andrestamatto.identityhub.clientapplication.domain.SpaSettings;
import java.util.Objects;
import java.util.UUID;

public final class ResolveOnboardingOrigin {

    private final ApplicationClientConfigurationRepository repository;

    public ResolveOnboardingOrigin(ApplicationClientConfigurationRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public Result execute(UUID machineClientId, UUID browserClientId, String redirectUri) {
        Objects.requireNonNull(machineClientId);
        Objects.requireNonNull(browserClientId);
        Objects.requireNonNull(redirectUri);
        var machine = configuration(machineClientId);
        var browser = configuration(browserClientId);
        if (!machine.client().enabled()
                || machine.projection().state() != ApplicationClientProjectionState.APPLIED
                || !(machine.client().settings() instanceof MachineSettings settings)
                || !settings.scopes().contains(MachineScope.ONBOARDING_WRITE)
                || !browser.client().enabled()
                || browser.projection().state() != ApplicationClientProjectionState.APPLIED
                || !machine.client().applicationId().equals(browser.client().applicationId())
                || !hasRedirect(browser.client(), redirectUri)) {
            throw new OnboardingOriginRejectedException();
        }
        return new Result(machine.client().applicationId().value());
    }

    private ApplicationClientConfiguration configuration(UUID clientId) {
        return repository.findById(new ApplicationClientId(clientId))
                .orElseThrow(OnboardingOriginRejectedException::new);
    }

    private boolean hasRedirect(ApplicationClient client, String redirectUri) {
        return switch (client.settings()) {
            case SpaSettings spa -> spa.redirectUris().stream()
                    .anyMatch(configured -> configured.value().equals(redirectUri));
            case BffSettings bff -> bff.redirectUris().stream()
                    .anyMatch(configured -> configured.value().equals(redirectUri));
            default -> false;
        };
    }

    public record Result(UUID applicationId) {
    }
}
