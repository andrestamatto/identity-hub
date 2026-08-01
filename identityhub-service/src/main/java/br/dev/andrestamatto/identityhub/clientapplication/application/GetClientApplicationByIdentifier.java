package br.dev.andrestamatto.identityhub.clientapplication.application;

import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationIdentifier;
import java.util.Objects;

public final class GetClientApplicationByIdentifier {

    private final ClientApplicationRepository repository;

    public GetClientApplicationByIdentifier(ClientApplicationRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public ClientApplicationSnapshot execute(String applicationIdentifier) {
        final ApplicationIdentifier identifier;
        try {
            identifier = new ApplicationIdentifier(applicationIdentifier);
        } catch (IllegalArgumentException exception) {
            throw new ClientApplicationUnavailableException();
        }
        return repository.findByIdentifier(identifier)
                .map(ClientApplicationSnapshot::from)
                .orElseThrow(ClientApplicationUnavailableException::new);
    }
}
