package br.dev.andrestamatto.identityhub.clientapplication.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationIdentifier;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplication;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplicationId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplicationState;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RegisterClientApplicationTest {

    private static final UUID APPLICATION_ID =
            UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0");
    private static final UUID OTHER_APPLICATION_ID =
            UUID.fromString("f61a9c64-4794-48d9-aa72-74951e0888b6");
    private static final Instant REGISTERED_AT =
            Instant.parse("2026-07-30T14:00:00Z");

    private final InMemoryClientApplicationRepository repository =
            new InMemoryClientApplicationRepository();
    private final RegisterClientApplication register = new RegisterClientApplication(
            repository,
            Clock.fixed(REGISTERED_AT, ZoneOffset.UTC));

    @Test
    void registersDraftApplication() {
        var result = register.execute(command(APPLICATION_ID, "auto-radar", "Auto Radar"));

        assertThat(result.created()).isTrue();
        assertThat(result.application().applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(result.application().identifier()).isEqualTo("auto-radar");
        assertThat(result.application().displayName()).isEqualTo("Auto Radar");
        assertThat(result.application().state()).isEqualTo(ClientApplicationState.DRAFT);
        assertThat(result.application().registeredAt()).isEqualTo(REGISTERED_AT);
        assertThat(repository.additions()).isEqualTo(1);
    }

    @Test
    void returnsStableResultForSemanticallyIdenticalRetry() {
        var firstResult = register.execute(command(APPLICATION_ID, "auto-radar", " Auto Radar "));
        var retriedResult = register.execute(command(APPLICATION_ID, "auto-radar", "Auto Radar"));

        assertThat(retriedResult.application()).isEqualTo(firstResult.application());
        assertThat(firstResult.created()).isTrue();
        assertThat(retriedResult.created()).isFalse();
        assertThat(repository.additions()).isEqualTo(1);
    }

    @Test
    void returnsWinningResultForConcurrentIdenticalRegistration() {
        var concurrentRepository = new ConcurrentWinningRepository();
        var concurrentRegister = new RegisterClientApplication(
                concurrentRepository,
                Clock.fixed(REGISTERED_AT, ZoneOffset.UTC));

        var result = concurrentRegister.execute(
                command(APPLICATION_ID, "auto-radar", "Auto Radar"));

        assertThat(result.application()).isEqualTo(
                ClientApplicationSnapshot.from(concurrentRepository.application));
        assertThat(result.created()).isFalse();
    }

    @Test
    void rejectsReuseOfApplicationIdWithDifferentContent() {
        register.execute(command(APPLICATION_ID, "auto-radar", "Auto Radar"));

        assertThatThrownBy(() -> register.execute(
                        command(APPLICATION_ID, "another-app", "Another App")))
                .isInstanceOf(ClientApplicationConflictException.class)
                .hasMessageContaining("application id");
        assertThat(repository.additions()).isEqualTo(1);
    }

    @Test
    void rejectsIdentifierAlreadyAssignedToAnotherApplication() {
        register.execute(command(APPLICATION_ID, "auto-radar", "Auto Radar"));

        assertThatThrownBy(() -> register.execute(
                        command(OTHER_APPLICATION_ID, "auto-radar", "Auto Radar")))
                .isInstanceOf(ClientApplicationConflictException.class)
                .hasMessageContaining("identifier");
        assertThat(repository.additions()).isEqualTo(1);
    }

    private RegisterClientApplication.Command command(
            UUID applicationId,
            String identifier,
            String displayName) {
        return new RegisterClientApplication.Command(applicationId, identifier, displayName);
    }

    private static final class InMemoryClientApplicationRepository
            implements ClientApplicationRepository {

        private final Map<ClientApplicationId, ClientApplication> applications =
                new LinkedHashMap<>();
        private int additions;

        @Override
        public Optional<ClientApplication> findById(ClientApplicationId id) {
            return Optional.ofNullable(applications.get(id));
        }

        @Override
        public Optional<ClientApplication> findByIdentifier(
                ApplicationIdentifier identifier) {
            return applications.values().stream()
                    .filter(application -> application.identifier().equals(identifier))
                    .findFirst();
        }

        @Override
        public void add(ClientApplication application) {
            applications.put(application.id(), application);
            additions++;
        }

        @Override
        public void updateSelfRegistrationPolicy(ClientApplication application) {
            throw new UnsupportedOperationException();
        }

        int additions() {
            return additions;
        }
    }

    private static final class ConcurrentWinningRepository
            implements ClientApplicationRepository {

        private ClientApplication application;

        @Override
        public Optional<ClientApplication> findById(ClientApplicationId id) {
            return Optional.ofNullable(application);
        }

        @Override
        public Optional<ClientApplication> findByIdentifier(
                ApplicationIdentifier identifier) {
            return Optional.empty();
        }

        @Override
        public void add(ClientApplication addedApplication) {
            application = addedApplication;
            throw new ClientApplicationConflictException(
                    "Concurrent registration won the unique constraint");
        }

        @Override
        public void updateSelfRegistrationPolicy(ClientApplication updatedApplication) {
            throw new UnsupportedOperationException();
        }
    }
}
