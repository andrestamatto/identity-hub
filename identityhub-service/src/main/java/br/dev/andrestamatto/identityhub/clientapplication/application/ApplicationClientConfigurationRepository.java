package br.dev.andrestamatto.identityhub.clientapplication.application;

import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientKey;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplicationId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.TokenAudience;
import java.util.Optional;

public interface ApplicationClientConfigurationRepository {

    Optional<ApplicationClientConfiguration> findById(ApplicationClientId id);

    Optional<ApplicationClientConfiguration> findByKey(
            ClientApplicationId applicationId,
            ApplicationClientKey key);

    Optional<ApplicationClientConfiguration> findByAudience(TokenAudience audience);

    void add(ApplicationClientConfiguration configuration);
}
