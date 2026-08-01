package br.dev.andrestamatto.identityhub.clientapplication.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationIdentifier;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplication;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplicationId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.DisplayName;
import br.dev.andrestamatto.identityhub.clientapplication.domain.SelfRegistrationPolicy;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConfigureSelfRegistrationTest {

    private static final UUID APPLICATION_ID =
            UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0");

    @Test
    void enablesSelfRegistrationForKnownApplication() {
        var repository = new RecordingRepository(application());
        var useCase = new ConfigureSelfRegistration(repository);

        var result = useCase.execute(APPLICATION_ID, "ENABLED");

        assertThat(result.selfRegistrationPolicy()).isEqualTo(SelfRegistrationPolicy.ENABLED);
        assertThat(repository.updates).isOne();
    }

    @Test
    void identicalReplayDoesNotWriteAgain() {
        var application = application();
        application.configureSelfRegistration(SelfRegistrationPolicy.ENABLED);
        var repository = new RecordingRepository(application);

        var result = new ConfigureSelfRegistration(repository)
                .execute(APPLICATION_ID, "ENABLED");

        assertThat(result.selfRegistrationPolicy()).isEqualTo(SelfRegistrationPolicy.ENABLED);
        assertThat(repository.updates).isZero();
    }

    @Test
    void rejectsUnknownPolicyWithoutWriting() {
        var repository = new RecordingRepository(application());

        assertThatThrownBy(() -> new ConfigureSelfRegistration(repository)
                        .execute(APPLICATION_ID, "UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(repository.updates).isZero();
    }

    private ClientApplication application() {
        return ClientApplication.register(
                new ClientApplicationId(APPLICATION_ID),
                new ApplicationIdentifier("auto-radar"),
                new DisplayName("Auto Radar"),
                Clock.fixed(Instant.parse("2026-07-31T18:00:00Z"), ZoneOffset.UTC));
    }

    private static final class RecordingRepository implements ClientApplicationRepository {
        private final ClientApplication application;
        private int updates;

        private RecordingRepository(ClientApplication application) {
            this.application = application;
        }

        @Override
        public Optional<ClientApplication> findById(ClientApplicationId id) {
            return Optional.ofNullable(application);
        }

        @Override
        public Optional<ClientApplication> findByIdentifier(ApplicationIdentifier identifier) {
            return Optional.empty();
        }

        @Override
        public void add(ClientApplication addedApplication) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateSelfRegistrationPolicy(ClientApplication updatedApplication) {
            updates++;
        }
    }
}
