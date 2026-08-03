package br.dev.andrestamatto.identityhub.clientapplication.adapter.out.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientConfiguration;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientProjection;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientSnapshot;
import br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationConflictException;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClient;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientKey;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationIdentifier;
import br.dev.andrestamatto.identityhub.clientapplication.domain.BffSettings;
import br.dev.andrestamatto.identityhub.clientapplication.domain.BrowserTransportPolicy;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplication;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplicationId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.MachineClientScope;
import br.dev.andrestamatto.identityhub.clientapplication.domain.MachineSettings;
import br.dev.andrestamatto.identityhub.clientapplication.domain.DisplayName;
import br.dev.andrestamatto.identityhub.clientapplication.domain.TokenAudience;
import br.dev.andrestamatto.identityhub.clientapplication.domain.SpaSettings;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class JdbcApplicationClientConfigurationRepositoryTest {

    private static final UUID APPLICATION_ID =
            UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0");
    private static final UUID CLIENT_ID =
            UUID.fromString("ff7c4748-f053-4fb6-91be-d34cf0015834");
    private static final UUID OTHER_CLIENT_ID =
            UUID.fromString("011fc7ce-c10b-47ed-a1b8-2a98e8b849ca");
    private static final UUID OPERATION_ID =
            UUID.fromString("27f3aa0b-6a70-43bd-a087-d5bc0c1bc779");
    private static final Instant NOW = Instant.parse("2026-07-31T14:00:00Z");

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17.10"));

    private static JdbcClient jdbcClient;
    private static JdbcClientApplicationRepository applicationRepository;
    private static JdbcApplicationClientConfigurationRepository repository;

    @BeforeAll
    static void migrateDatabase() {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).load().migrate();
        jdbcClient = JdbcClient.create(dataSource);
        applicationRepository = new JdbcClientApplicationRepository(jdbcClient);
        repository = new JdbcApplicationClientConfigurationRepository(
                jdbcClient,
                new TransactionTemplate(new JdbcTransactionManager(dataSource)));
    }

    @BeforeEach
    void clearAndRegisterApplication() {
        jdbcClient.sql("delete from application_client_projection_outbox").update();
        jdbcClient.sql("delete from application_client").update();
        jdbcClient.sql("delete from client_application").update();
        applicationRepository.add(application());
    }

    @Test
    void atomicallyRoundTripsClientAndPendingProjection() {
        var configuration = configuration(CLIENT_ID, OPERATION_ID, "catalog-api", "catalog-api");

        repository.add(configuration);

        assertThat(repository.findById(new ApplicationClientId(CLIENT_ID)))
                .map(ApplicationClientSnapshot::from)
                .contains(ApplicationClientSnapshot.from(configuration));
        assertThat(repository.findByKey(
                        new ClientApplicationId(APPLICATION_ID),
                        new ApplicationClientKey("catalog-api")))
                .isPresent();
        assertThat(repository.findByAudience(new TokenAudience("catalog-api")))
                .isPresent();
        assertThat(numberOfClients()).isEqualTo(1);
        assertThat(numberOfOperations()).isEqualTo(1);
    }

    @Test
    void rollsBackClientWhenOutboxInsertFails() {
        repository.add(configuration(CLIENT_ID, OPERATION_ID, "catalog-api", "catalog-api"));

        assertThatThrownBy(() -> repository.add(configuration(
                        OTHER_CLIENT_ID,
                        OPERATION_ID,
                        "another-api",
                        "another-api")))
                .isInstanceOf(ClientApplicationConflictException.class);

        assertThat(repository.findById(new ApplicationClientId(OTHER_CLIENT_ID))).isEmpty();
        assertThat(numberOfClients()).isEqualTo(1);
        assertThat(numberOfOperations()).isEqualTo(1);
    }

    @Test
    void rejectsAudienceAlreadyAssignedToAnotherClient() {
        repository.add(configuration(CLIENT_ID, OPERATION_ID, "catalog-api", "catalog-api"));

        assertThatThrownBy(() -> repository.add(configuration(
                        OTHER_CLIENT_ID,
                        UUID.randomUUID(),
                        "another-api",
                        "catalog-api")))
                .isInstanceOf(ClientApplicationConflictException.class);
    }

    @Test
    void reservesDueProjectionForOnlyOneWorkerAndAppliesIt() {
        repository.add(configuration(CLIENT_ID, OPERATION_ID, "catalog-api", "catalog-api"));
        var firstWorker = UUID.randomUUID();
        var secondWorker = UUID.randomUUID();

        assertThat(repository.reserveNext(firstWorker, NOW, Duration.ofSeconds(30)))
                .map(configuration -> configuration.projection().operationId())
                .contains(OPERATION_ID);
        assertThat(repository.reserveNext(secondWorker, NOW, Duration.ofSeconds(30)))
                .isEmpty();

        repository.markApplied(OPERATION_ID, firstWorker, NOW.plusSeconds(1));

        var projection = repository.findById(new ApplicationClientId(CLIENT_ID))
                .orElseThrow()
                .projection();
        assertThat(projection.state())
                .isEqualTo(br.dev.andrestamatto.identityhub.clientapplication.application
                        .ApplicationClientProjectionState.APPLIED);
        assertThat(projection.attempts()).isOne();
    }

    @Test
    void makesTransientFailureEligibleOnlyAfterBackoff() {
        repository.add(configuration(CLIENT_ID, OPERATION_ID, "catalog-api", "catalog-api"));
        var worker = UUID.randomUUID();
        var retryAt = NOW.plusSeconds(10);
        repository.reserveNext(worker, NOW, Duration.ofSeconds(30)).orElseThrow();

        repository.scheduleRetry(
                OPERATION_ID,
                worker,
                1,
                retryAt,
                "KEYCLOAK_UNAVAILABLE",
                NOW.plusSeconds(1));

        assertThat(repository.reserveNext(UUID.randomUUID(), NOW.plusSeconds(9), Duration.ofSeconds(30)))
                .isEmpty();
        assertThat(repository.reserveNext(UUID.randomUUID(), retryAt, Duration.ofSeconds(30)))
                .isPresent();
    }

    @Test
    void recoversProjectionAfterWorkerLeaseExpires() {
        repository.add(configuration(CLIENT_ID, OPERATION_ID, "catalog-api", "catalog-api"));
        repository.reserveNext(UUID.randomUUID(), NOW, Duration.ofSeconds(30)).orElseThrow();

        assertThat(repository.reserveNext(
                        UUID.randomUUID(),
                        NOW.plusSeconds(31),
                        Duration.ofSeconds(30)))
                .isPresent();
    }

    @Test
    void requeuesAppliedProjectionForExplicitReconciliation() {
        repository.add(configuration(CLIENT_ID, OPERATION_ID, "catalog-api", "catalog-api"));
        var worker = UUID.randomUUID();
        repository.reserveNext(worker, NOW, Duration.ofSeconds(30)).orElseThrow();
        repository.markApplied(OPERATION_ID, worker, NOW.plusSeconds(1));

        var reconciled = repository.requeue(
                        new ApplicationClientId(CLIENT_ID),
                        NOW.plusSeconds(2))
                .orElseThrow();

        assertThat(reconciled.projection().state())
                .isEqualTo(br.dev.andrestamatto.identityhub.clientapplication.application
                        .ApplicationClientProjectionState.PENDING);
        assertThat(reconciled.projection().attempts()).isZero();
        assertThat(reconciled.projection().nextAttemptAt()).isEqualTo(NOW.plusSeconds(2));
    }

    @Test
    void atomicallyRoundTripsSpaEndpointsInDeclaredOrder() {
        var configuration = spaConfiguration();

        repository.add(configuration);

        var stored = repository.findById(new ApplicationClientId(CLIENT_ID)).orElseThrow();
        assertThat(ApplicationClientSnapshot.from(stored).redirectUris()).containsExactly(
                "http://127.0.0.1:5173/auth/callback",
                "http://127.0.0.1:5173/auth/silent-callback");
        assertThat(ApplicationClientSnapshot.from(stored).webOrigins())
                .containsExactly("http://127.0.0.1:5173");
        assertThat(ApplicationClientSnapshot.from(stored).audience()).isNull();
        assertThat(numberOfOperations()).isOne();
    }

    @Test
    void atomicallyRoundTripsBffRedirectsWithoutAudienceOrOrigins() {
        var configuration = bffConfiguration();

        repository.add(configuration);

        var stored = repository.findById(new ApplicationClientId(CLIENT_ID)).orElseThrow();
        assertThat(ApplicationClientSnapshot.from(stored).type()).isEqualTo("BFF");
        assertThat(ApplicationClientSnapshot.from(stored).redirectUris()).containsExactly(
                "http://127.0.0.1:8081/login/oauth2/code/identityhub");
        assertThat(ApplicationClientSnapshot.from(stored).webOrigins()).isEmpty();
        assertThat(ApplicationClientSnapshot.from(stored).audience()).isNull();
        assertThat(numberOfOperations()).isOne();
    }

    @Test
    void atomicallyRoundTripsMachineWithoutBrowserOrApiSettings() {
        var client = application().configureMachine(
                new ApplicationClientId(CLIENT_ID),
                new ApplicationClientKey("catalog-membership-provisioner"),
                new MachineSettings(java.util.List.of(MachineClientScope.MEMBERSHIP_WRITE)),
                Clock.fixed(NOW, ZoneOffset.UTC));
        var configuration = new ApplicationClientConfiguration(
                client,
                ApplicationClientProjection.pending(
                        OPERATION_ID,
                        client.id(),
                        "jdbc-machine-projection",
                        NOW));

        repository.add(configuration);

        jdbcClient.sql("""
                        insert into application_client_machine_scope (
                            application_client_id, position, scope
                        ) values (:clientId, 0, 'onboarding:write')
                        """)
                .param("clientId", CLIENT_ID)
                .update();

        var stored = repository.findById(new ApplicationClientId(CLIENT_ID)).orElseThrow();
        var snapshot = ApplicationClientSnapshot.from(stored);
        assertThat(snapshot.type()).isEqualTo("MACHINE");
        assertThat(snapshot.audience()).isNull();
        assertThat(snapshot.redirectUris()).isEmpty();
        assertThat(snapshot.webOrigins()).isEmpty();
        assertThat(snapshot.scopes()).containsExactly("membership:write");
        assertThat(numberOfOperations()).isOne();
    }

    private ApplicationClientConfiguration configuration(
            UUID clientId,
            UUID operationId,
            String key,
            String audience) {
        ApplicationClient client = application().configureProtectedApi(
                new ApplicationClientId(clientId),
                new ApplicationClientKey(key),
                new TokenAudience(audience),
                Clock.fixed(NOW, ZoneOffset.UTC));
        return new ApplicationClientConfiguration(
                client,
                ApplicationClientProjection.pending(
                        operationId,
                        client.id(),
                        "jdbc-projection-test",
                        NOW));
    }

    private ApplicationClientConfiguration spaConfiguration() {
        var client = application().configureSpa(
                new ApplicationClientId(CLIENT_ID),
                new ApplicationClientKey("catalog-web"),
                SpaSettings.create(
                        java.util.List.of(
                                "http://127.0.0.1:5173/auth/callback",
                                "http://127.0.0.1:5173/auth/silent-callback"),
                        java.util.List.of("http://127.0.0.1:5173"),
                        BrowserTransportPolicy.DEVELOPMENT),
                Clock.fixed(NOW, ZoneOffset.UTC));
        return new ApplicationClientConfiguration(
                client,
                ApplicationClientProjection.pending(
                        OPERATION_ID,
                        client.id(),
                        "jdbc-spa-projection",
                        NOW));
    }

    private ApplicationClientConfiguration bffConfiguration() {
        var client = application().configureBff(
                new ApplicationClientId(CLIENT_ID),
                new ApplicationClientKey("catalog-bff"),
                BffSettings.create(
                        java.util.List.of(
                                "http://127.0.0.1:8081/login/oauth2/code/identityhub"),
                        BrowserTransportPolicy.DEVELOPMENT),
                Clock.fixed(NOW, ZoneOffset.UTC));
        return new ApplicationClientConfiguration(
                client,
                ApplicationClientProjection.pending(
                        OPERATION_ID,
                        client.id(),
                        "jdbc-bff-projection",
                        NOW));
    }

    private ClientApplication application() {
        return ClientApplication.register(
                new ClientApplicationId(APPLICATION_ID),
                new ApplicationIdentifier("social-catalog"),
                new DisplayName("Social Catalog"),
                Clock.fixed(Instant.parse("2026-07-31T12:00:00Z"), ZoneOffset.UTC));
    }

    private int numberOfClients() {
        return jdbcClient.sql("select count(*) from application_client")
                .query(Integer.class)
                .single();
    }

    private int numberOfOperations() {
        return jdbcClient.sql("select count(*) from application_client_projection_outbox")
                .query(Integer.class)
                .single();
    }
}
