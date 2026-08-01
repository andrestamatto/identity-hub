package br.dev.andrestamatto.identityhub.communication.adapter.out.clientapplication;

import br.dev.andrestamatto.identityhub.clientapplication.application.GetClientApplication;
import br.dev.andrestamatto.identityhub.communication.application.EmailOrigin;
import br.dev.andrestamatto.identityhub.communication.application.EmailOriginResolver;
import java.util.Objects;
import java.util.UUID;

public final class ClientApplicationEmailOriginResolver implements EmailOriginResolver {

    private final GetClientApplication getClientApplication;
    private final String environment;

    public ClientApplicationEmailOriginResolver(
            GetClientApplication getClientApplication,
            String environment) {
        this.getClientApplication = Objects.requireNonNull(getClientApplication);
        this.environment = Objects.requireNonNull(environment);
    }

    @Override
    public EmailOrigin resolve(UUID applicationId) {
        var application = getClientApplication.execute(applicationId);
        return new EmailOrigin(
                application.applicationId(),
                application.identifier(),
                application.displayName(),
                environment);
    }
}
