package br.dev.andrestamatto.identityhub.clientapplication.application;

import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClient;
import java.util.Objects;

public record ApplicationClientConfiguration(
        ApplicationClient client,
        ApplicationClientProjection projection) {

    public ApplicationClientConfiguration {
        Objects.requireNonNull(client);
        Objects.requireNonNull(projection);
        if (!client.id().equals(projection.clientId())) {
            throw new IllegalArgumentException(
                    "Application client and projection must reference the same client");
        }
    }
}
