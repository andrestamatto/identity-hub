package br.dev.andrestamatto.identityhub.clientapplication.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationIdentifier;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplication;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplicationId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.DisplayName;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetClientApplicationTest {

    private static final UUID APPLICATION_ID =
            UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0");
    private static final Instant REGISTERED_AT =
            Instant.parse("2026-07-30T14:00:00Z");

    @Test
    void returnsRegisteredApplication() {
        var application = ClientApplication.register(
                new ClientApplicationId(APPLICATION_ID),
                new ApplicationIdentifier("auto-radar"),
                new DisplayName("Auto Radar"),
                Clock.fixed(REGISTERED_AT, ZoneOffset.UTC));
        var getApplication = new GetClientApplication(new SingleApplicationRepository(application));

        var result = getApplication.execute(APPLICATION_ID);

        assertThat(result).isEqualTo(ClientApplicationSnapshot.from(application));
    }

    @Test
    void rejectsUnknownApplicationId() {
        var getApplication = new GetClientApplication(new SingleApplicationRepository(null));

        assertThatThrownBy(() -> getApplication.execute(APPLICATION_ID))
                .isInstanceOf(ClientApplicationNotFoundException.class)
                .hasMessageContaining(APPLICATION_ID.toString());
    }

    private record SingleApplicationRepository(ClientApplication application)
            implements ClientApplicationRepository {

        @Override
        public Optional<ClientApplication> findById(ClientApplicationId id) {
            return Optional.ofNullable(application)
                    .filter(candidate -> candidate.id().equals(id));
        }

        @Override
        public Optional<ClientApplication> findByIdentifier(
                ApplicationIdentifier identifier) {
            return Optional.ofNullable(application)
                    .filter(candidate -> candidate.identifier().equals(identifier));
        }

        @Override
        public void add(ClientApplication addedApplication) {
            throw new UnsupportedOperationException();
        }
    }
}
