package br.dev.andrestamatto.identityhub.clientapplication.application;

import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientId;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public final class ReconcileApplicationClientProjection {

    private final ApplicationClientConfigurationRepository configurationRepository;
    private final ApplicationClientProjectionRepository projectionRepository;
    private final Clock clock;

    public ReconcileApplicationClientProjection(
            ApplicationClientConfigurationRepository configurationRepository,
            ApplicationClientProjectionRepository projectionRepository,
            Clock clock) {
        this.configurationRepository = Objects.requireNonNull(configurationRepository);
        this.projectionRepository = Objects.requireNonNull(projectionRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    public ApplicationClientSnapshot execute(UUID applicationId, UUID clientId) {
        Objects.requireNonNull(applicationId);
        Objects.requireNonNull(clientId);
        var id = new ApplicationClientId(clientId);
        var current = configurationRepository.findById(id)
                .orElseThrow(() -> new ApplicationClientNotFoundException(clientId));
        if (!current.client().applicationId().value().equals(applicationId)) {
            throw new ApplicationClientNotFoundException(clientId);
        }
        return projectionRepository.requeue(id, clock.instant())
                .map(ApplicationClientSnapshot::from)
                .orElseThrow(() -> new ApplicationClientNotFoundException(clientId));
    }
}
