package br.dev.andrestamatto.identityhub.clientapplication.application;

import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.BffSettings;
import br.dev.andrestamatto.identityhub.clientapplication.domain.MachineSettings;
import java.util.Objects;
import java.util.UUID;

public final class RotateConfidentialClientSecret {

    private final ApplicationClientConfigurationRepository repository;
    private final ConfidentialClientSecretRotator rotator;

    public RotateConfidentialClientSecret(
            ApplicationClientConfigurationRepository repository,
            ConfidentialClientSecretRotator rotator) {
        this.repository = Objects.requireNonNull(repository);
        this.rotator = Objects.requireNonNull(rotator);
    }

    public ConfidentialClientSecret execute(UUID applicationId, UUID clientId) {
        Objects.requireNonNull(applicationId);
        Objects.requireNonNull(clientId);
        var configuration = repository.findById(new ApplicationClientId(clientId))
                .orElseThrow(() -> new ApplicationClientNotFoundException(clientId));
        var client = configuration.client();
        if (!client.applicationId().value().equals(applicationId)) {
            throw new ApplicationClientNotFoundException(clientId);
        }
        if (!(client.settings() instanceof BffSettings
                || client.settings() instanceof MachineSettings)) {
            throw new IllegalArgumentException("Only a confidential client has a secret");
        }
        if (!client.enabled()
                || configuration.projection().state() != ApplicationClientProjectionState.APPLIED) {
            throw new ClientApplicationConflictException(
                    "Confidential client must be enabled and projected before secret rotation");
        }
        return rotator.rotate(ApplicationClientSnapshot.from(configuration));
    }
}
