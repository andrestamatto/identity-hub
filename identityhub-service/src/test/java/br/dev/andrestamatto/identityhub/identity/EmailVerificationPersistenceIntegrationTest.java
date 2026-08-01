package br.dev.andrestamatto.identityhub.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.dev.andrestamatto.identityhub.communication.adapter.out.jdbc.JdbcEmailDeliveryRepository;
import br.dev.andrestamatto.identityhub.communication.application.EmailOrigin;
import br.dev.andrestamatto.identityhub.communication.application.RequestEmailVerificationEmail;
import br.dev.andrestamatto.identityhub.identity.adapter.out.communication.CommunicationVerificationEmailRequester;
import br.dev.andrestamatto.identityhub.identity.adapter.out.jdbc.JdbcEmailVerificationChallengeRepository;
import br.dev.andrestamatto.identityhub.identity.adapter.out.jdbc.SpringVerificationTransaction;
import br.dev.andrestamatto.identityhub.identity.application.EmailVerificationSecret;
import br.dev.andrestamatto.identityhub.identity.application.RequestEmailVerification;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
class EmailVerificationPersistenceIntegrationTest {

    private static final UUID APPLICATION_ID =
            UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0");
    private static final UUID USER_REF =
            UUID.fromString("9bc4a8c9-405b-4f4a-b443-3c2012369264");
    private static final UUID CHALLENGE_ID =
            UUID.fromString("27f3aa0b-6a70-43bd-a087-d5bc0c1bc779");
    private static final Instant NOW = Instant.parse("2026-07-31T18:00:00Z");
    private static final String SECRET = "test-only-verification-secret";

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17.10"));

    private static JdbcClient jdbcClient;
    private static TransactionTemplate transactions;

    @BeforeAll
    static void migrateDatabase() {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).load().migrate();
        jdbcClient = JdbcClient.create(dataSource);
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
    void commitsChallengeAndEmailOutboxInOneTransaction() {
        var request = request(command -> emailRequester().request(command));

        request.execute(command(USER_REF));

        assertThat(jdbcClient.sql("select count(*) from email_verification_challenge")
                .query(Long.class)
                .single()).isOne();
        var digest = jdbcClient.sql("""
                        select encode(secret_digest, 'hex')
                        from email_verification_challenge
                        where challenge_id = :challengeId
                        """)
                .param("challengeId", CHALLENGE_ID)
                .query(String.class)
                .single();
        assertThat(digest).hasSize(64).doesNotContain(SECRET);
        assertThat(jdbcClient.sql("""
                        select sensitive_content
                        from email_delivery_outbox
                        where delivery_id = :deliveryId
                        """)
                .param("deliveryId", CHALLENGE_ID)
                .query(String.class)
                .single())
                .isEqualTo("https://auth.dev.example.test/verify-email#token="
                        + CHALLENGE_ID + "." + SECRET);
    }

    @Test
    void rollsBackChallengeWhenEmailOutboxRequestFails() {
        var request = request(command -> {
            throw new IllegalStateException("synthetic delivery failure");
        });

        assertThatThrownBy(() -> request.execute(command(UUID.randomUUID())))
                .isInstanceOf(IllegalStateException.class);

        assertThat(jdbcClient.sql("select count(*) from email_verification_challenge")
                .query(Long.class)
                .single()).isZero();
        assertThat(jdbcClient.sql("select count(*) from email_delivery_outbox")
                .query(Long.class)
                .single()).isZero();
    }

    private RequestEmailVerification request(
            br.dev.andrestamatto.identityhub.identity.application.VerificationEmailRequester
                    requester) {
        return new RequestEmailVerification(
                new JdbcEmailVerificationChallengeRepository(jdbcClient),
                requester,
                new SpringVerificationTransaction(transactions),
                () -> new EmailVerificationSecret(SECRET),
                Clock.fixed(NOW, ZoneOffset.UTC),
                () -> CHALLENGE_ID,
                URI.create("https://auth.dev.example.test"));
    }

    private CommunicationVerificationEmailRequester emailRequester() {
        var emailRepository = new JdbcEmailDeliveryRepository(jdbcClient, transactions);
        var requestEmail = new RequestEmailVerificationEmail(
                emailRepository,
                id -> new EmailOrigin(id, "auto-radar", "Auto Radar", "development"),
                Clock.fixed(NOW, ZoneOffset.UTC));
        return new CommunicationVerificationEmailRequester(requestEmail);
    }

    private RequestEmailVerification.Command command(UUID userRef) {
        return new RequestEmailVerification.Command(
                APPLICATION_ID,
                userRef,
                "andre@example.test",
                "verification-persistence");
    }
}
