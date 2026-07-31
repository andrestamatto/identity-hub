package br.dev.andrestamatto.identityhub.clientapplication.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientKey;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationIdentifier;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplication;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplicationId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.DisplayName;
import br.dev.andrestamatto.identityhub.clientapplication.domain.TokenAudience;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConfigureProtectedApiClientTest {

    private static final UUID APPLICATION_ID =
            UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0");
    private static final UUID CLIENT_ID =
            UUID.fromString("ff7c4748-f053-4fb6-91be-d34cf0015834");
    private static final UUID OPERATION_ID =
            UUID.fromString("27f3aa0b-6a70-43bd-a087-d5bc0c1bc779");
    private static final Instant CONFIGURED_AT =
            Instant.parse("2026-07-31T14:00:00.123456Z");

    private final InMemoryApplicationRepository applicationRepository =
            new InMemoryApplicationRepository();
    private final InMemoryApplicationClientConfigurationRepository clientRepository =
            new InMemoryApplicationClientConfigurationRepository();
    private final ConfigureProtectedApiClient configure = new ConfigureProtectedApiClient(
            applicationRepository,
            clientRepository,
            Clock.fixed(CONFIGURED_AT, ZoneOffset.UTC),
            () -> OPERATION_ID);

    @Test
    void configuresEnabledApiWithPendingProjection() {
        applicationRepository.application = application();

        var result = configure.execute(command(
                CLIENT_ID, "social-catalog-api", "social-catalog-api"));

        assertThat(result.created()).isTrue();
        assertThat(result.client().applicationClientId()).isEqualTo(CLIENT_ID);
        assertThat(result.client().applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(result.client().key()).isEqualTo("social-catalog-api");
        assertThat(result.client().type()).isEqualTo("API");
        assertThat(result.client().audience()).isEqualTo("social-catalog-api");
        assertThat(result.client().enabled()).isTrue();
        assertThat(result.client().configuredAt()).isEqualTo(CONFIGURED_AT);
        assertThat(result.client().operationId()).isEqualTo(OPERATION_ID);
        assertThat(result.client().projectionState())
                .isEqualTo(ApplicationClientProjectionState.PENDING);
        assertThat(clientRepository.additions).isEqualTo(1);
    }

    @Test
    void identicalRetryReturnsPendingConfigurationWithoutAnotherOperation() {
        applicationRepository.application = application();

        var first = configure.execute(command(
                CLIENT_ID, "social-catalog-api", "social-catalog-api"));
        var replay = configure.execute(command(
                CLIENT_ID, "social-catalog-api", "social-catalog-api"));

        assertThat(replay.created()).isFalse();
        assertThat(replay.client()).isEqualTo(first.client());
        assertThat(clientRepository.additions).isEqualTo(1);
    }

    @Test
    void rejectsConfigurationForUnknownApplication() {
        assertThatThrownBy(() -> configure.execute(command(
                        CLIENT_ID, "social-catalog-api", "social-catalog-api")))
                .isInstanceOf(ClientApplicationNotFoundException.class);
    }

    @Test
    void rejectsKeyAlreadyAssignedInsideApplication() {
        applicationRepository.application = application();
        configure.execute(command(CLIENT_ID, "social-catalog-api", "catalog-api"));

        assertThatThrownBy(() -> configure.execute(command(
                        UUID.randomUUID(), "social-catalog-api", "another-api")))
                .isInstanceOf(ClientApplicationConflictException.class)
                .hasMessageContaining("key");
    }

    @Test
    void rejectsAudienceAlreadyAssignedInEnvironment() {
        applicationRepository.application = application();
        configure.execute(command(CLIENT_ID, "social-catalog-api", "catalog-api"));

        assertThatThrownBy(() -> configure.execute(command(
                        UUID.randomUUID(), "another-api", "catalog-api")))
                .isInstanceOf(ClientApplicationConflictException.class)
                .hasMessageContaining("audience");
    }

    private ConfigureProtectedApiClient.Command command(
            UUID clientId,
            String key,
            String audience) {
        return new ConfigureProtectedApiClient.Command(APPLICATION_ID, clientId, key, audience);
    }

    private ClientApplication application() {
        return ClientApplication.register(
                new ClientApplicationId(APPLICATION_ID),
                new ApplicationIdentifier("social-catalog"),
                new DisplayName("Social Catalog"),
                Clock.fixed(Instant.parse("2026-07-31T12:00:00Z"), ZoneOffset.UTC));
    }

    private static final class InMemoryApplicationRepository
            implements ClientApplicationRepository {

        private ClientApplication application;

        @Override
        public Optional<ClientApplication> findById(ClientApplicationId id) {
            return Optional.ofNullable(application)
                    .filter(found -> found.id().equals(id));
        }

        @Override
        public Optional<ClientApplication> findByIdentifier(ApplicationIdentifier identifier) {
            return Optional.empty();
        }

        @Override
        public void add(ClientApplication addedApplication) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class InMemoryApplicationClientConfigurationRepository
            implements ApplicationClientConfigurationRepository {

        private final Map<ApplicationClientId, ApplicationClientConfiguration> configurations =
                new LinkedHashMap<>();
        private int additions;

        @Override
        public Optional<ApplicationClientConfiguration> findById(ApplicationClientId id) {
            return Optional.ofNullable(configurations.get(id));
        }

        @Override
        public Optional<ApplicationClientConfiguration> findByKey(
                ClientApplicationId applicationId,
                ApplicationClientKey key) {
            return configurations.values().stream()
                    .filter(configuration -> configuration.client().applicationId().equals(applicationId))
                    .filter(configuration -> configuration.client().key().equals(key))
                    .findFirst();
        }

        @Override
        public Optional<ApplicationClientConfiguration> findByAudience(TokenAudience audience) {
            return configurations.values().stream()
                    .filter(configuration -> configuration.client().audience().equals(audience))
                    .findFirst();
        }

        @Override
        public void add(ApplicationClientConfiguration configuration) {
            configurations.put(configuration.client().id(), configuration);
            additions++;
        }
    }
}
