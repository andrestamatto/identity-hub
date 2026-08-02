package br.dev.andrestamatto.identityhub.identity.adapter.out.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import br.dev.andrestamatto.identityhub.clientapplication.adapter.out.jdbc.JdbcApplicationClientConfigurationRepository;
import br.dev.andrestamatto.identityhub.clientapplication.adapter.out.jdbc.JdbcClientApplicationRepository;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientConfiguration;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientProjection;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientKey;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationIdentifier;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplication;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplicationId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.DisplayName;
import br.dev.andrestamatto.identityhub.clientapplication.domain.MachineSettings;
import br.dev.andrestamatto.identityhub.identity.application.OnboardingSessionRepository;
import br.dev.andrestamatto.identityhub.identity.domain.OnboardingDigest;
import br.dev.andrestamatto.identityhub.identity.domain.OnboardingSession;
import br.dev.andrestamatto.identityhub.identity.domain.OnboardingSessionId;
import br.dev.andrestamatto.identityhub.identity.domain.PkceCodeChallenge;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
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
class JdbcOnboardingSessionRepositoryTest {

    private static final UUID APPLICATION_ID = UUID.randomUUID();
    private static final UUID MACHINE_CLIENT_ID = UUID.randomUUID();
    private static final UUID BROWSER_CLIENT_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-08-01T20:00:00Z");

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17.10"));

    private static JdbcClient jdbcClient;
    private static JdbcOnboardingSessionRepository repository;

    @BeforeAll
    static void migrate() {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).load().migrate();
        jdbcClient = JdbcClient.create(dataSource);
        var transactions = new TransactionTemplate(new JdbcTransactionManager(dataSource));
        repository = new JdbcOnboardingSessionRepository(jdbcClient);
        seedClients(jdbcClient, transactions);
    }

    @BeforeEach
    void clearSessions() {
        jdbcClient.sql("delete from onboarding_session").update();
    }

    @Test
    void atomicallyCreatesAndReturnsStableReplay() {
        var first = repository.saveOrFind(session("A", "a"));
        var replay = repository.saveOrFind(session("B", "a"));

        assertThat(first.created()).isTrue();
        assertThat(replay.created()).isFalse();
        assertThat(replay.session().id()).isEqualTo(first.session().id());
        assertThat(numberOfSessions()).isOne();
        assertThat(storedAcquisitionDigest()).isEqualTo("c".repeat(64));
    }

    @Test
    void leavesSemanticCollisionForApplicationLayerToReject() {
        repository.saveOrFind(session("A", "a"));

        var stored = repository.saveOrFind(session("B", "b"));

        assertThat(stored.created()).isFalse();
        assertThat(stored.session().requestDigest().value()).isEqualTo("a".repeat(64));
        assertThat(numberOfSessions()).isOne();
    }

    @Test
    void concurrentRetriesCreateOnlyOneSession() throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> saveWhenReleased(session("A", "a"), ready, start));
            var second = executor.submit(() -> saveWhenReleased(session("B", "a"), ready, start));
            ready.await();
            start.countDown();

            assertThat(List.of(first.get().created(), second.get().created()))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(first.get().session().id()).isEqualTo(second.get().session().id());
            assertThat(numberOfSessions()).isOne();
        }
    }

    private OnboardingSessionRepository.SaveResult saveWhenReleased(
            OnboardingSession session,
            CountDownLatch ready,
            CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        return repository.saveOrFind(session);
    }

    private OnboardingSession session(String idSeed, String requestDigestSeed) {
        return OnboardingSession.initiate(
                new OnboardingSessionId(idSeed.repeat(43)),
                APPLICATION_ID,
                MACHINE_CLIENT_ID,
                BROWSER_CLIENT_ID,
                new OnboardingDigest("c".repeat(64)),
                "https://app.example.com/auth/callback",
                new PkceCodeChallenge("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"),
                new OnboardingDigest("d".repeat(64)),
                new OnboardingDigest(requestDigestSeed.repeat(64)),
                "jdbc-onboarding-test",
                NOW);
    }

    private static void seedClients(
            JdbcClient jdbcClient, TransactionTemplate transactions) {
        var applications = new JdbcClientApplicationRepository(jdbcClient);
        var clients = new JdbcApplicationClientConfigurationRepository(jdbcClient, transactions);
        var application = ClientApplication.register(
                new ClientApplicationId(APPLICATION_ID),
                new ApplicationIdentifier("jdbc-onboarding"),
                new DisplayName("JDBC Onboarding"),
                Clock.fixed(NOW, ZoneOffset.UTC));
        applications.add(application);
        addClient(clients, application, MACHINE_CLIENT_ID, "onboarding-machine");
        addClient(clients, application, BROWSER_CLIENT_ID, "onboarding-browser");
    }

    private static void addClient(
            JdbcApplicationClientConfigurationRepository clients,
            ClientApplication application,
            UUID clientId,
            String key) {
        var client = application.configureMachine(
                new ApplicationClientId(clientId),
                new ApplicationClientKey(key),
                MachineSettings.create(List.of("onboarding:write")),
                Clock.fixed(NOW, ZoneOffset.UTC));
        clients.add(new ApplicationClientConfiguration(
                client,
                ApplicationClientProjection.pending(
                        UUID.randomUUID(), client.id(), "jdbc-onboarding-seed", NOW)));
    }

    private int numberOfSessions() {
        return jdbcClient.sql("select count(*) from onboarding_session")
                .query(Integer.class)
                .single();
    }

    private String storedAcquisitionDigest() {
        return jdbcClient.sql("select acquisition_reference_digest from onboarding_session")
                .query(String.class)
                .single();
    }
}
