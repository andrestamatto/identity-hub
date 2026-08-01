package br.dev.andrestamatto.identityhub.communication.adapter.out.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import br.dev.andrestamatto.identityhub.communication.application.EmailDelivery;
import br.dev.andrestamatto.identityhub.communication.application.EmailDeliveryPurpose;
import br.dev.andrestamatto.identityhub.communication.application.EmailDeliveryState;
import br.dev.andrestamatto.identityhub.communication.application.EmailOrigin;
import br.dev.andrestamatto.identityhub.communication.domain.EmailDeliveryId;
import br.dev.andrestamatto.identityhub.communication.domain.EmailRecipient;
import java.time.Duration;
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
class JdbcEmailDeliveryRepositoryTest {

    private static final UUID APPLICATION_ID =
            UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0");
    private static final EmailDeliveryId DELIVERY_ID = new EmailDeliveryId(
            UUID.fromString("27f3aa0b-6a70-43bd-a087-d5bc0c1bc779"));
    private static final UUID WORKER_ID =
            UUID.fromString("87ba5cc1-ae98-4a85-aeb0-103103d5bd23");
    private static final Instant NOW = Instant.parse("2026-07-31T18:00:00Z");

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17.10"));

    private static JdbcClient jdbcClient;
    private static JdbcEmailDeliveryRepository repository;

    @BeforeAll
    static void migrateDatabase() {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).load().migrate();
        jdbcClient = JdbcClient.create(dataSource);
        repository = new JdbcEmailDeliveryRepository(
                jdbcClient,
                new TransactionTemplate(new DataSourceTransactionManager(dataSource)));
    }

    @BeforeEach
    void resetDatabase() {
        jdbcClient.sql("delete from email_delivery_outbox").update();
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
    void persistsReservesAndCompletesDelivery() {
        repository.add(delivery());

        var reserved = repository.reserveNext(WORKER_ID, NOW, Duration.ofSeconds(30));

        assertThat(reserved).contains(delivery());
        repository.markDelivered(DELIVERY_ID, WORKER_ID, NOW.plusSeconds(1));
        assertThat(repository.find(DELIVERY_ID)).get()
                .extracting(EmailDelivery::state, EmailDelivery::attempts)
                .containsExactly(EmailDeliveryState.DELIVERED, 1);
    }

    @Test
    void leasePreventsSecondWorkerFromReservingSameDelivery() {
        repository.add(delivery());
        repository.reserveNext(WORKER_ID, NOW, Duration.ofSeconds(30));

        assertThat(repository.reserveNext(UUID.randomUUID(), NOW, Duration.ofSeconds(30)))
                .isEmpty();
    }

    @Test
    void failedDeliveryCanBeRequeuedWithoutLosingIdentity() {
        repository.add(delivery());
        repository.reserveNext(WORKER_ID, NOW, Duration.ofSeconds(30));
        repository.markFailed(
                DELIVERY_ID, WORKER_ID, 1, "INVALID_MESSAGE", NOW.plusSeconds(1));

        assertThat(repository.requeue(DELIVERY_ID, NOW.plusSeconds(2))).get()
                .satisfies(value -> {
                    assertThat(value.state()).isEqualTo(EmailDeliveryState.PENDING);
                    assertThat(value.attempts()).isZero();
                    assertThat(value.lastFailureCode()).isNull();
                });
    }

    private EmailDelivery delivery() {
        return EmailDelivery.request(
                DELIVERY_ID,
                new EmailOrigin(APPLICATION_ID, "auto-radar", "Auto Radar", "development"),
                new EmailRecipient("andre@example.com"),
                EmailDeliveryPurpose.PASSWORD_CHANGED,
                "correlation-123",
                NOW);
    }
}
