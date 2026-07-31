package br.dev.andrestamatto.identityhub.clientapplication.application;

import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationIdentifier;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplication;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplicationId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.DisplayName;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public final class RegisterClientApplication {

    private final ClientApplicationRepository repository;
    private final Clock clock;

    public RegisterClientApplication(
            ClientApplicationRepository repository,
            Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
    }

    public ClientApplicationRegistration execute(Command command) {
        Objects.requireNonNull(command);
        var id = new ClientApplicationId(command.applicationId());
        var identifier = new ApplicationIdentifier(command.identifier());
        var displayName = new DisplayName(command.displayName());

        var applicationWithId = repository.findById(id);
        if (applicationWithId.isPresent()) {
            return replay(applicationWithId.orElseThrow(), identifier, displayName);
        }
        if (repository.findByIdentifier(identifier).isPresent()) {
            throw new ClientApplicationConflictException(
                    "Client application identifier is already assigned");
        }

        var application = ClientApplication.register(id, identifier, displayName, clock);
        try {
            repository.add(application);
        } catch (ClientApplicationConflictException conflict) {
            var concurrentWinner = repository.findById(id);
            if (concurrentWinner.isPresent()) {
                return replay(
                        concurrentWinner.orElseThrow(),
                        identifier,
                        displayName);
            }
            throw conflict;
        }
        return new ClientApplicationRegistration(
                ClientApplicationSnapshot.from(application),
                true);
    }

    private ClientApplicationRegistration replay(
            ClientApplication existing,
            ApplicationIdentifier identifier,
            DisplayName displayName) {
        if (existing.identifier().equals(identifier)
                && existing.displayName().equals(displayName)) {
            return new ClientApplicationRegistration(
                    ClientApplicationSnapshot.from(existing),
                    false);
        }
        throw new ClientApplicationConflictException(
                "Client application id is already assigned to different content");
    }

    public record Command(
            UUID applicationId,
            String identifier,
            String displayName) {
    }
}
