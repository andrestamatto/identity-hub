package br.dev.andrestamatto.identityhub.clientapplication.application;

import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplicationId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.SelfRegistrationPolicy;
import java.util.Objects;
import java.util.UUID;

public final class ConfigureSelfRegistration {

    private final ClientApplicationRepository repository;

    public ConfigureSelfRegistration(ClientApplicationRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public ClientApplicationSnapshot execute(UUID applicationId, String policyValue) {
        var id = new ClientApplicationId(applicationId);
        var policy = SelfRegistrationPolicy.from(policyValue);
        var application = repository.findById(id)
                .orElseThrow(() -> new ClientApplicationNotFoundException(applicationId));
        if (application.configureSelfRegistration(policy)) {
            repository.updateSelfRegistrationPolicy(application);
        }
        return ClientApplicationSnapshot.from(application);
    }
}
