package br.dev.andrestamatto.identityhub.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.dev.andrestamatto.identityhub.communication.adapter.out.jdbc.JdbcEmailDeliveryRepository;
import br.dev.andrestamatto.identityhub.communication.application.EmailOrigin;
import br.dev.andrestamatto.identityhub.communication.application.RequestPasswordRecoveryEmail;
import br.dev.andrestamatto.identityhub.communication.application.RequestPasswordChangedEmail;
import br.dev.andrestamatto.identityhub.identity.adapter.out.communication.CommunicationPasswordChangedNotifier;
import br.dev.andrestamatto.identityhub.identity.adapter.out.communication.CommunicationRecoveryEmailRequester;
import br.dev.andrestamatto.identityhub.identity.adapter.out.jdbc.JdbcPasswordRecoveryChallengeRepository;
import br.dev.andrestamatto.identityhub.identity.adapter.out.jdbc.SpringVerificationTransaction;
import br.dev.andrestamatto.identityhub.identity.application.PasswordRecoveryIdentity;
import br.dev.andrestamatto.identityhub.identity.application.CompletePasswordRecovery;
import br.dev.andrestamatto.identityhub.identity.application.PasswordRecoveryRateLimitException;
import br.dev.andrestamatto.identityhub.identity.application.PasswordRecoverySecret;
import br.dev.andrestamatto.identityhub.identity.application.RecoveryEmailRequester;
import br.dev.andrestamatto.identityhub.identity.application.RequestPasswordRecovery;
import br.dev.andrestamatto.identityhub.identity.domain.LoginEmail;
import br.dev.andrestamatto.identityhub.identity.domain.UserAccountRef;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
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
class PasswordRecoveryPersistenceIntegrationTest {

    private static final UUID APPLICATION_ID =
            UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0");
    private static final UUID USER_REF =
            UUID.fromString("9bc4a8c9-405b-4f4a-b443-3c2012369264");
    private static final UUID CHALLENGE_ID =
            UUID.fromString("27f3aa0b-6a70-43bd-a087-d5bc0c1bc779");
    private static final Instant NOW = Instant.parse("2026-08-02T16:00:00Z");
    private static final String SECRET = "test-only-password-recovery-secret";
    private static final UUID PASSWORD_CHANGED_DELIVERY_ID =
            UUID.fromString("c0616535-c869-4554-8df8-c220dca39b8e");

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
        jdbcClient.sql("delete from password_recovery_challenge").update();
        jdbcClient.sql("delete from email_verification_challenge").update();
        jdbcClient.sql("delete from client_application").update();
        jdbcClient.sql("""
                insert into client_application (id, identifier, display_name, state, registered_at)
                values (:id, 'auto-radar', 'Auto Radar', 'DRAFT', :registeredAt)
                """)
                .param("id", APPLICATION_ID)
                .param("registeredAt", java.time.OffsetDateTime.parse("2026-08-02T15:00:00Z"))
                .update();
    }

    @Test
    void commitsHashedChallengeAndEmailOutboxInOneTransaction() {
        request(CHALLENGE_ID, emailRequester()).execute(command());

        var digest = jdbcClient.sql("""
                        select encode(secret_digest, 'hex')
                        from password_recovery_challenge
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
                .isEqualTo("https://auth.dev.example.test/recover-password#token="
                        + CHALLENGE_ID + "." + SECRET);
    }

    @Test
    void rollsBackChallengeWhenEmailOutboxRequestFails() {
        RecoveryEmailRequester failingRequester = command -> {
            throw new IllegalStateException("synthetic delivery failure");
        };

        assertThatThrownBy(() -> request(CHALLENGE_ID, failingRequester).execute(command()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(jdbcClient.sql("select count(*) from password_recovery_challenge")
                .query(Long.class).single()).isZero();
        assertThat(jdbcClient.sql("select count(*) from email_delivery_outbox")
                .query(Long.class).single()).isZero();
    }

    @Test
    void supersedesPreviousProofAndLimitsDestinationToThreeRequests() {
        var sequence = new AtomicInteger();
        var ids = new UUID[] {
            CHALLENGE_ID,
            UUID.fromString("b0d3144f-61ed-4e97-a8b9-fbd5f61a8f02"),
            UUID.fromString("c0638275-c1d8-46ba-a43d-ae61dfe42ffc"),
            UUID.fromString("d873cc54-e528-4cc7-8274-401053fd8e87")
        };
        var request = request(() -> ids[sequence.getAndIncrement()], emailRequester());

        request.execute(command());
        request.execute(command());
        request.execute(command());

        assertThatThrownBy(() -> request.execute(command()))
                .isInstanceOf(PasswordRecoveryRateLimitException.class);
        assertThat(jdbcClient.sql("""
                        select count(*) from password_recovery_challenge
                        where state = 'ACTIVE'
                        """).query(Long.class).single()).isOne();
        assertThat(jdbcClient.sql("select count(*) from password_recovery_challenge")
                .query(Long.class).single()).isEqualTo(3);
    }

    @Test
    void commitsConsumedProofAndPasswordChangedNotificationAfterReset() {
        request(CHALLENGE_ID, emailRequester()).execute(command());
        var resets = new AtomicInteger();
        var emailRepository = new JdbcEmailDeliveryRepository(jdbcClient, transactions);
        var changedEmail = new RequestPasswordChangedEmail(
                emailRepository,
                id -> new EmailOrigin(id, "auto-radar", "Auto Radar", "development"),
                Clock.fixed(NOW.plusSeconds(1), ZoneOffset.UTC));
        var complete = new CompletePasswordRecovery(
                new JdbcPasswordRecoveryChallengeRepository(jdbcClient),
                (account, email, password) -> resets.incrementAndGet(),
                new CommunicationPasswordChangedNotifier(changedEmail),
                new SpringVerificationTransaction(transactions),
                Clock.fixed(NOW.plusSeconds(1), ZoneOffset.UTC),
                () -> PASSWORD_CHANGED_DELIVERY_ID);

        complete.execute(new CompletePasswordRecovery.Command(
                CHALLENGE_ID + "." + SECRET,
                "a new secure password phrase".toCharArray(),
                "complete-password-recovery-persistence"));

        assertThat(resets).hasValue(1);
        assertThat(jdbcClient.sql("""
                        select state
                        from password_recovery_challenge
                        where challenge_id = :challengeId
                        """)
                .param("challengeId", CHALLENGE_ID)
                .query(String.class)
                .single())
                .isEqualTo("USED");
        assertThat(jdbcClient.sql("""
                        select purpose
                        from email_delivery_outbox
                        where delivery_id = :deliveryId
                        """)
                .param("deliveryId", PASSWORD_CHANGED_DELIVERY_ID)
                .query(String.class)
                .single())
                .isEqualTo("PASSWORD_CHANGED");
    }

    private RequestPasswordRecovery request(UUID id, RecoveryEmailRequester requester) {
        return request(() -> id, requester);
    }

    private RequestPasswordRecovery request(
            java.util.function.Supplier<UUID> ids,
            RecoveryEmailRequester requester) {
        return new RequestPasswordRecovery(
                email -> Optional.of(new PasswordRecoveryIdentity(
                        new UserAccountRef(USER_REF), new LoginEmail("andre@example.test"))),
                new JdbcPasswordRecoveryChallengeRepository(jdbcClient),
                requester,
                new SpringVerificationTransaction(transactions),
                () -> new PasswordRecoverySecret(SECRET),
                Clock.fixed(NOW, ZoneOffset.UTC),
                ids,
                URI.create("https://auth.dev.example.test"));
    }

    private CommunicationRecoveryEmailRequester emailRequester() {
        var emailRepository = new JdbcEmailDeliveryRepository(jdbcClient, transactions);
        var requestEmail = new RequestPasswordRecoveryEmail(
                emailRepository,
                id -> new EmailOrigin(id, "auto-radar", "Auto Radar", "development"),
                Clock.fixed(NOW, ZoneOffset.UTC));
        return new CommunicationRecoveryEmailRequester(requestEmail);
    }

    private RequestPasswordRecovery.Command command() {
        return new RequestPasswordRecovery.Command(
                APPLICATION_ID, "andre@example.test", "password-recovery-persistence");
    }
}
