package br.dev.andrestamatto.identityhub.clientapplication.adapter.out.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationTokenClient;
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
class JdbcApplicationTokenClientResolverTest {

    private static final UUID APPLICATION_ID =
            UUID.fromString("7ce22c46-a1c8-4974-b3fe-cc077e23f356");
    private static final UUID OTHER_APPLICATION_ID =
            UUID.fromString("8cc90298-874e-4f85-bcad-28ab63e760fb");
    private static final UUID API_ID =
            UUID.fromString("20e6f465-84be-4c97-b8fd-37b618d2bd91");
    private static final UUID SPA_ID =
            UUID.fromString("81bd51f0-280d-48bd-9e16-4763667a1c10");

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17.10"));

    private static JdbcClient jdbcClient;
    private static JdbcApplicationTokenClientResolver resolver;

    @BeforeAll
    static void migrateDatabase() {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).load().migrate();
        jdbcClient = JdbcClient.create(dataSource);
        resolver = new JdbcApplicationTokenClientResolver(jdbcClient);
    }

    @BeforeEach
    void clearDatabase() {
        jdbcClient.sql("delete from application_client_projection_outbox").update();
        jdbcClient.sql("delete from application_client").update();
        jdbcClient.sql("delete from client_application").update();
        insertApplication(APPLICATION_ID, "catalog");
        insertApplication(OTHER_APPLICATION_ID, "other");
    }

    @Test
    void resolvesOnlyEnabledAppliedPublicTokenClientsFromRequestedApplication() {
        insertClient(API_ID, APPLICATION_ID, "catalog-api", "API", "catalog-api", true, "APPLIED");
        insertClient(SPA_ID, APPLICATION_ID, "catalog-web", "SPA", null, true, "APPLIED");
        insertClient(UUID.randomUUID(), APPLICATION_ID, "pending-api", "API", "pending-api", true, "PENDING");
        insertClient(UUID.randomUUID(), APPLICATION_ID, "disabled-api", "API", "disabled-api", false, "APPLIED");
        insertClient(UUID.randomUUID(), APPLICATION_ID, "machine", "MACHINE", null, true, "APPLIED");
        insertClient(UUID.randomUUID(), OTHER_APPLICATION_ID, "other-api", "API", "other-api", true, "APPLIED");

        assertThat(resolver.resolve(APPLICATION_ID)).containsExactlyInAnyOrder(
                new ApplicationTokenClient(API_ID, "API", "catalog-api"),
                new ApplicationTokenClient(SPA_ID, "SPA", null));
    }

    private void insertApplication(UUID id, String identifier) {
        jdbcClient.sql("""
                        insert into client_application (
                            id, identifier, display_name, state, registered_at)
                        values (:id, :identifier, :identifier, 'DRAFT', now())
                        """)
                .param("id", id)
                .param("identifier", identifier)
                .update();
    }

    private void insertClient(
            UUID id,
            UUID applicationId,
            String key,
            String type,
            String audience,
            boolean enabled,
            String projectionState) {
        jdbcClient.sql("""
                        insert into application_client (
                            id, application_id, client_key, client_type, audience,
                            enabled, configured_at)
                        values (:id, :applicationId, :key, :type, :audience,
                            :enabled, now())
                        """)
                .param("id", id)
                .param("applicationId", applicationId)
                .param("key", key)
                .param("type", type)
                .param("audience", audience)
                .param("enabled", enabled)
                .update();
        jdbcClient.sql("""
                        insert into application_client_projection_outbox (
                            operation_id, application_client_id, state, attempts,
                            next_attempt_at, created_at, updated_at, payload_version,
                            correlation_id)
                        values (:operationId, :clientId, :state, 0, now(), now(), now(),
                            1, 'token-resolver-test')
                        """)
                .param("operationId", UUID.randomUUID())
                .param("clientId", id)
                .param("state", projectionState)
                .update();
    }
}
