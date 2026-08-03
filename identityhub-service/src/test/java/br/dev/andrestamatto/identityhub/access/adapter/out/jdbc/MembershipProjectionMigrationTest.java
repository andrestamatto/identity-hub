package br.dev.andrestamatto.identityhub.access.adapter.out.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class MembershipProjectionMigrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17.10"));

    @Test
    void backfillsPendingMembershipsFromVersionSeventeen() {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).target("17").load().migrate();
        var jdbc = JdbcClient.create(dataSource);
        var now = OffsetDateTime.ofInstant(
                Instant.parse("2026-08-03T01:00:00Z"), ZoneOffset.UTC);
        var applicationId = UUID.randomUUID();
        var clientId = UUID.randomUUID();
        var membershipId = UUID.randomUUID();
        jdbc.sql("""
                        insert into client_application
                            (id, identifier, display_name, state, registered_at)
                        values (:id, 'legacy-pending', 'Legacy Pending', 'DRAFT', :now)
                        """)
                .param("id", applicationId)
                .param("now", now)
                .update();
        jdbc.sql("""
                        insert into application_client
                            (id, application_id, client_key, client_type, enabled, configured_at)
                        values (:id, :applicationId, 'provisioner', 'MACHINE', true, :now)
                        """)
                .param("id", clientId)
                .param("applicationId", applicationId)
                .param("now", now)
                .update();
        jdbc.sql("""
                        insert into membership
                            (id, application_id, user_account_ref, state, requested_at, updated_at)
                        values (:id, :applicationId, :userId, 'PENDING', :now, :now)
                        """)
                .param("id", membershipId)
                .param("applicationId", applicationId)
                .param("userId", UUID.randomUUID())
                .param("now", now)
                .update();
        jdbc.sql("""
                        insert into membership_grant_operation (
                            operation_id, membership_id, application_client_id,
                            idempotency_key, command_fingerprint, correlation_id, accepted_at
                        ) values (
                            :operationId, :membershipId, :clientId,
                            'migration-backfill-001', :fingerprint, 'migration-backfill', :now
                        )
                        """)
                .param("operationId", UUID.randomUUID())
                .param("membershipId", membershipId)
                .param("clientId", clientId)
                .param("fingerprint", "a".repeat(64))
                .param("now", now)
                .update();

        Flyway.configure().dataSource(dataSource).load().migrate();

        assertThat(jdbc.sql("""
                        select state || ':' || correlation_id
                        from membership_projection_outbox
                        where membership_id = :membershipId
                        """)
                .param("membershipId", membershipId)
                .query(String.class)
                .single()).isEqualTo("PENDING:migration-backfill");
    }
}
