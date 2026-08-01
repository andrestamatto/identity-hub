package br.dev.andrestamatto.identityhub.identity.adapter.out.clientapplication;

import br.dev.andrestamatto.identityhub.clientapplication.application.GetClientApplication;
import br.dev.andrestamatto.identityhub.clientapplication.domain.SelfRegistrationPolicy;
import br.dev.andrestamatto.identityhub.identity.application.SelfRegistrationPolicyResolver;
import java.util.Objects;
import java.util.UUID;

public final class ClientApplicationSelfRegistrationPolicyResolver
        implements SelfRegistrationPolicyResolver {

    private final GetClientApplication getClientApplication;

    public ClientApplicationSelfRegistrationPolicyResolver(
            GetClientApplication getClientApplication) {
        this.getClientApplication = Objects.requireNonNull(getClientApplication);
    }

    @Override
    public boolean isEnabled(UUID applicationId) {
        return getClientApplication.execute(applicationId).selfRegistrationPolicy()
                == SelfRegistrationPolicy.ENABLED;
    }
}
