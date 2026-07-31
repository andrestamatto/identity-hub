package br.dev.andrestamatto.identityhub.audit.adapter.out.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import br.dev.andrestamatto.identityhub.audit.application.AdministrativeAccessEvent;
import br.dev.andrestamatto.identityhub.audit.application.AdministrativeAccessOutcome;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class JdbcAdministrativeAccessEventRepositoryTest {

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17.10"));

    private static JdbcClient jdbcClient;

    @BeforeAll
    static void migrateDatabase() {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).load().migrate();
        jdbcClient = JdbcClient.create(dataSource);
    }

    @Test
    void appendsAnImmutableAdministrativeAccessEvent() {
        var repository = new JdbcAdministrativeAccessEventRepository(jdbcClient);
        var event = new AdministrativeAccessEvent(
                UUID.fromString("d00e951b-adf7-4ba3-8358-a31e69999266"),
                Instant.parse("2026-07-29T12:00:00Z"),
                "correlation-123",
                "operator-id",
                "GET",
                "/internal/admin/runtime",
                AdministrativeAccessOutcome.ALLOWED,
                "authorized");

        repository.append(event);

        var stored = jdbcClient.sql("""
                        select correlation_id, actor_subject, outcome
                        from administrative_access_event
                        where id = :id
                        """)
                .param("id", event.id())
                .query(StoredEvent.class)
                .single();

        assertThat(stored).isEqualTo(new StoredEvent(
                "correlation-123",
                "operator-id",
                "ALLOWED"));
    }

    private record StoredEvent(String correlationId, String actorSubject, String outcome) {
    }
}
