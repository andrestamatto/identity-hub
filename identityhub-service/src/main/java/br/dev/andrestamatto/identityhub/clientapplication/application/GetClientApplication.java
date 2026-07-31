package br.dev.andrestamatto.identityhub.clientapplication.application;

import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplicationId;
import java.util.Objects;
import java.util.UUID;

public final class GetClientApplication {

    private final ClientApplicationRepository repository;

    public GetClientApplication(ClientApplicationRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public ClientApplicationSnapshot execute(UUID applicationId) {
        var id = new ClientApplicationId(applicationId);
        return repository.findById(id)
                .map(ClientApplicationSnapshot::from)
                .orElseThrow(() -> new ClientApplicationNotFoundException(applicationId));
    }
}
