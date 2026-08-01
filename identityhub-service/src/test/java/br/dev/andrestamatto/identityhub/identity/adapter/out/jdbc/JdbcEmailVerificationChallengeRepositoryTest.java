package br.dev.andrestamatto.identityhub.identity.adapter.out.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.dev.andrestamatto.identityhub.identity.application.EmailVerificationRateLimitException;
import br.dev.andrestamatto.identityhub.identity.domain.EmailVerificationChallenge;
import br.dev.andrestamatto.identityhub.identity.domain.EmailVerificationState;
import br.dev.andrestamatto.identityhub.identity.domain.LoginEmail;
import br.dev.andrestamatto.identityhub.identity.domain.UserAccountRef;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class JdbcEmailVerificationChallengeRepositoryTest {

    private static final UUID APPLICATION_ID =
            UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0");
    private static final UserAccountRef USER_REF = new UserAccountRef(
            UUID.fromString("9bc4a8c9-405b-4f4a-b443-3c2012369264"));
    private static final Instant NOW = Instant.parse("2026-07-31T18:00:00Z");

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17.10"));

    private static JdbcClient jdbcClient;
    private static JdbcEmailVerificationChallengeRepository repository;
    private static TransactionTemplate transactions;

    @BeforeAll
    static void migrateDatabase() {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).load().migrate();
        jdbcClient = JdbcClient.create(dataSource);
        repository = new JdbcEmailVerificationChallengeRepository(jdbcClient);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @BeforeEach
    void resetDatabase() {
        jdbcClient.sql("delete from email_delivery_outbox").update();
        jdbcClient.sql("delete from email_verification_challenge").update();
        jdbcClient.sql("delete from client_application").update();
        jdbcClient.sql("""
                insert into client_application (id, identifier, display_name, state, registered_at)
                values (:id, 'auto-radar', 'Auto Radar', 'DRAFT', :registeredAt)
                """)
                .param("id", APPLICATION_ID)
                .param("registeredAt", java.time.OffsetDateTime.parse("2026-07-31T17:00:00Z"))
                .update();
    }

    @Test
    void replacesActiveChallengeAndPersistsSingleUseState() {
        var first = challenge(UUID.randomUUID(), NOW);
        var second = challenge(UUID.randomUUID(), NOW.plusSeconds(60));

        replace(first);
        replace(second);

        assertThat(repository.findForUpdate(first.id())).get()
                .extracting(EmailVerificationChallenge::state)
                .isEqualTo(EmailVerificationState.SUPERSEDED);
        var persisted = repository.findForUpdate(second.id()).orElseThrow();
        persisted.markUsed(NOW.plusSeconds(120));
        repository.update(persisted);
        assertThat(repository.findForUpdate(second.id())).get()
                .satisfies(value -> {
                    assertThat(value.state()).isEqualTo(EmailVerificationState.USED);
                    assertThat(value.usedAt()).isEqualTo(NOW.plusSeconds(120));
                });
    }

    @Test
    void limitsFourthRequestInsideFifteenMinuteWindow() {
        replace(challenge(UUID.randomUUID(), NOW));
        replace(challenge(UUID.randomUUID(), NOW.plusSeconds(60)));
        replace(challenge(UUID.randomUUID(), NOW.plusSeconds(120)));

        assertThatThrownBy(() -> replace(challenge(UUID.randomUUID(), NOW.plusSeconds(180))))
                .isInstanceOf(EmailVerificationRateLimitException.class);
        assertThat(jdbcClient.sql("select count(*) from email_verification_challenge")
                .query(Long.class)
                .single()).isEqualTo(3);
    }

    private void replace(EmailVerificationChallenge challenge) {
        transactions.executeWithoutResult(status -> repository.replaceActive(
                challenge, challenge.createdAt().minusSeconds(900), 3));
    }

    private EmailVerificationChallenge challenge(UUID id, Instant createdAt) {
        return EmailVerificationChallenge.start(
                id,
                USER_REF,
                APPLICATION_ID,
                new LoginEmail("andre@example.test"),
                new byte[32],
                createdAt,
                createdAt.plusSeconds(1800));
    }
}
