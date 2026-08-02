package br.dev.andrestamatto.identityhub.identity.adapter.out.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import br.dev.andrestamatto.identityhub.identity.application.GlobalAccountDisableOperation;
import br.dev.andrestamatto.identityhub.identity.application.GlobalAccountDisableStatus;
import br.dev.andrestamatto.identityhub.identity.domain.UserAccountRef;
import java.time.Instant;
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
class JdbcGlobalAccountDisableOperationRepositoryTest {

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17.10"));

    private static JdbcClient jdbcClient;

    @BeforeAll
    static void migrateDatabase() {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).load().migrate();
        jdbcClient = JdbcClient.create(dataSource);
    }

    @BeforeEach
    void resetDatabase() {
        jdbcClient.sql("delete from global_account_disable_operation").update();
    }

    @Test
    void savesAndUpdatesTheAuditableOperationWithoutLosingItsIdentity() {
        var repository = new JdbcGlobalAccountDisableOperationRepository(jdbcClient);
        var pending = operation(GlobalAccountDisableStatus.PENDING, null);

        repository.save(pending);
        repository.save(pending.completed(Instant.parse("2026-08-02T18:01:00Z")));

        var stored = repository.findByIdempotencyKey("disable-account-001");
        assertThat(stored.operationId()).isEqualTo(pending.operationId());
        assertThat(stored.userAccountRef()).isEqualTo(pending.userAccountRef());
        assertThat(stored.actorSubject()).isEqualTo("admin-subject");
        assertThat(stored.reason()).isEqualTo("Confirmed security incident");
        assertThat(stored.correlationId()).isEqualTo("correlation-001");
        assertThat(stored.status()).isEqualTo(GlobalAccountDisableStatus.COMPLETED);
        assertThat(stored.completedAt()).isEqualTo(Instant.parse("2026-08-02T18:01:00Z"));
    }

    @Test
    void returnsNullForAnUnknownIdempotencyKey() {
        var repository = new JdbcGlobalAccountDisableOperationRepository(jdbcClient);

        assertThat(repository.findByIdempotencyKey("unknown-operation")).isNull();
    }

    private GlobalAccountDisableOperation operation(
            GlobalAccountDisableStatus status,
            Instant completedAt) {
        return new GlobalAccountDisableOperation(
                UUID.fromString("0658c077-6544-47fc-9755-d7491f07dc5b"),
                new UserAccountRef(UUID.fromString("f20ace9e-e02a-436f-93e6-edaff0320733")),
                "Confirmed security incident",
                "disable-account-001",
                "b9b8c6601d9ce559b9b8c6601d9ce559b9b8c6601d9ce559b9b8c6601d9ce559",
                "admin-subject",
                "correlation-001",
                status,
                null,
                Instant.parse("2026-08-02T18:00:00Z"),
                completedAt);
    }
}
