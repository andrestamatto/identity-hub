package br.dev.andrestamatto.identityhub.clientapplication.application;

import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationIdentifier;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplication;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplicationId;
import java.util.Optional;

public interface ClientApplicationRepository {

    Optional<ClientApplication> findById(ClientApplicationId id);

    Optional<ClientApplication> findByIdentifier(ApplicationIdentifier identifier);

    void add(ClientApplication application);

    void updateSelfRegistrationPolicy(ClientApplication application);
}
