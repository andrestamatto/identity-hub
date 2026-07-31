package br.dev.andrestamatto.identityhub.clientapplication.application;

import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientId;
import java.util.Objects;
import java.util.UUID;

public final class GetApplicationClientConfiguration {

    private final ApplicationClientConfigurationRepository repository;

    public GetApplicationClientConfiguration(ApplicationClientConfigurationRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public ApplicationClientSnapshot execute(UUID applicationId, UUID clientId) {
        Objects.requireNonNull(applicationId);
        Objects.requireNonNull(clientId);
        var snapshot = repository.findById(new ApplicationClientId(clientId))
                .map(ApplicationClientSnapshot::from)
                .orElseThrow(() -> new ApplicationClientNotFoundException(clientId));
        if (!snapshot.applicationId().equals(applicationId)) {
            throw new ApplicationClientNotFoundException(clientId);
        }
        return snapshot;
    }
}
