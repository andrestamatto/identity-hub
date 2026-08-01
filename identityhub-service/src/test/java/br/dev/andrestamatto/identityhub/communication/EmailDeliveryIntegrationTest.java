package br.dev.andrestamatto.identityhub.communication;

import static org.assertj.core.api.Assertions.assertThat;

import br.dev.andrestamatto.identityhub.communication.adapter.out.jdbc.JdbcEmailDeliveryRepository;
import br.dev.andrestamatto.identityhub.communication.adapter.out.smtp.SmtpEmailDeliverySender;
import br.dev.andrestamatto.identityhub.communication.application.EmailDeliveryResult;
import br.dev.andrestamatto.identityhub.communication.application.EmailOrigin;
import br.dev.andrestamatto.identityhub.communication.application.PasswordChangedEmailRenderer;
import br.dev.andrestamatto.identityhub.communication.application.ProcessEmailDelivery;
import br.dev.andrestamatto.identityhub.communication.application.RequestPasswordChangedEmail;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.containers.wait.strategy.Wait;

@Testcontainers(disabledWithoutDocker = true)
class EmailDeliveryIntegrationTest {

    private static final UUID APPLICATION_ID =
            UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0");
    private static final UUID DELIVERY_ID =
            UUID.fromString("27f3aa0b-6a70-43bd-a087-d5bc0c1bc779");
    private static final Instant NOW = Instant.parse("2026-07-31T18:00:00Z");

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17.10"));

    @Container
    private static final GenericContainer<?> MAILPIT = new GenericContainer<>(
            DockerImageName.parse("axllent/mailpit:v1.30.6"))
            .withExposedPorts(1025, 8025)
            .waitingFor(Wait.forHttp("/api/v1/messages").forPort(8025));

    private static JdbcEmailDeliveryRepository repository;

    @BeforeAll
    static void prepareDatabase() {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).load().migrate();
        var jdbcClient = JdbcClient.create(dataSource);
        jdbcClient.sql("""
                insert into client_application (id, identifier, display_name, state, registered_at)
                values (:id, 'auto-radar', 'Auto Radar', 'DRAFT', :registeredAt)
                """)
                .param("id", APPLICATION_ID)
                .param("registeredAt", java.time.OffsetDateTime.parse("2026-07-31T17:00:00Z"))
                .update();
        repository = new JdbcEmailDeliveryRepository(
                jdbcClient,
                new TransactionTemplate(new DataSourceTransactionManager(dataSource)));
    }

    @Test
    void persistsThenDeliversPasswordChangedEmailThroughRealSmtp() throws Exception {
        var clock = Clock.fixed(NOW, ZoneOffset.UTC);
        var request = new RequestPasswordChangedEmail(
                repository,
                id -> new EmailOrigin(id, "auto-radar", "Auto Radar", "development"),
                clock);
        request.execute(new RequestPasswordChangedEmail.Command(
                DELIVERY_ID, APPLICATION_ID, "andre@example.com", "integration-email"));

        var javaMailSender = new JavaMailSenderImpl();
        javaMailSender.setHost(MAILPIT.getHost());
        javaMailSender.setPort(MAILPIT.getMappedPort(1025));
        var processor = new ProcessEmailDelivery(
                repository,
                new SmtpEmailDeliverySender(javaMailSender, "identityhub@example.com"),
                new PasswordChangedEmailRenderer(),
                clock,
                Duration.ofSeconds(30),
                Duration.ofSeconds(5),
                3);

        assertThat(processor.processNext(UUID.randomUUID()))
                .isEqualTo(EmailDeliveryResult.DELIVERED);

        var response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(mailpitUri("/api/v1/messages")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("andre@example.com")
                .contains("[Auto Radar] Password changed");
    }

    private URI mailpitUri(String path) {
        return URI.create("http://" + MAILPIT.getHost() + ":"
                + MAILPIT.getMappedPort(8025) + path);
    }
}
