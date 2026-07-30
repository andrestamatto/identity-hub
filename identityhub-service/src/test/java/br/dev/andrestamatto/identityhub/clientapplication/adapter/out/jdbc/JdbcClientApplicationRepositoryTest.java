package br.dev.andrestamatto.identityhub.clientapplication.adapter.out.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationConflictException;
import br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationSnapshot;
import br.dev.andrestamatto.identityhub.clientapplication.application.RegisterClientApplication;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationIdentifier;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplication;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplicationId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.DisplayName;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class JdbcClientApplicationRepositoryTest {

    private static final Instant REGISTERED_AT =
            Instant.parse("2026-07-30T14:00:00Z");
    private static final UUID APPLICATION_ID =
            UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0");
    private static final UUID OTHER_APPLICATION_ID =
            UUID.fromString("f61a9c64-4794-48d9-aa72-74951e0888b6");

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17.10"));

    private static JdbcClient jdbcClient;
    private static JdbcClientApplicationRepository repository;

    @BeforeAll
    static void migrateDatabase() {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).load().migrate();
        jdbcClient = JdbcClient.create(dataSource);
        repository = new JdbcClientApplicationRepository(jdbcClient);
    }

    @BeforeEach
    void clearApplications() {
        jdbcClient.sql("delete from client_application").update();
    }

    @Test
    void roundTripsApplicationByIdAndIdentifier() {
        var application = application(APPLICATION_ID, "auto-radar", "Auto Radar");

        repository.add(application);

        assertThat(repository.findById(application.id()))
                .map(ClientApplicationSnapshot::from)
                .contains(ClientApplicationSnapshot.from(application));
        assertThat(repository.findByIdentifier(application.identifier()))
                .map(ClientApplicationSnapshot::from)
                .contains(ClientApplicationSnapshot.from(application));
    }

    @Test
    void keepsRegistrationIdempotentWithRealPostgreSql() {
        var register = new RegisterClientApplication(
                repository,
                Clock.fixed(REGISTERED_AT, ZoneOffset.UTC));
        var command = new RegisterClientApplication.Command(
                APPLICATION_ID,
                "auto-radar",
                "Auto Radar");

        var firstResult = register.execute(command);
        var retriedResult = register.execute(command);

        assertThat(retriedResult).isEqualTo(firstResult);
        assertThat(numberOfApplications()).isEqualTo(1);
    }

    @Test
    void rejectsIdentifierAlreadyStoredForAnotherApplication() {
        repository.add(application(APPLICATION_ID, "auto-radar", "Auto Radar"));

        assertThatThrownBy(() -> repository.add(
                        application(OTHER_APPLICATION_ID, "auto-radar", "Another App")))
                .isInstanceOf(ClientApplicationConflictException.class);
        assertThat(numberOfApplications()).isEqualTo(1);
    }

    @Test
    void rejectsApplicationIdAlreadyStoredWithDifferentContent() {
        repository.add(application(APPLICATION_ID, "auto-radar", "Auto Radar"));

        assertThatThrownBy(() -> repository.add(
                        application(APPLICATION_ID, "another-app", "Another App")))
                .isInstanceOf(ClientApplicationConflictException.class);
        assertThat(numberOfApplications()).isEqualTo(1);
    }

    private ClientApplication application(
            UUID id,
            String identifier,
            String displayName) {
        return ClientApplication.register(
                new ClientApplicationId(id),
                new ApplicationIdentifier(identifier),
                new DisplayName(displayName),
                Clock.fixed(REGISTERED_AT, ZoneOffset.UTC));
    }

    private int numberOfApplications() {
        return jdbcClient.sql("select count(*) from client_application")
                .query(Integer.class)
                .single();
    }
}
