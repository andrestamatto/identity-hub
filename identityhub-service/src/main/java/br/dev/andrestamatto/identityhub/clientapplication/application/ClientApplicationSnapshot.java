package br.dev.andrestamatto.identityhub.clientapplication.application;

import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplication;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplicationState;
import java.time.Instant;
import java.util.UUID;

public record ClientApplicationSnapshot(
        UUID applicationId,
        String identifier,
        String displayName,
        ClientApplicationState state,
        Instant registeredAt) {

    public static ClientApplicationSnapshot from(ClientApplication application) {
        return new ClientApplicationSnapshot(
                application.id().value(),
                application.identifier().value(),
                application.displayName().value(),
                application.state(),
                application.registeredAt());
    }
}
