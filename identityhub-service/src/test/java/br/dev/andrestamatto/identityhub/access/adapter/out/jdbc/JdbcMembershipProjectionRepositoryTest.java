package br.dev.andrestamatto.identityhub.access.adapter.out.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.dev.andrestamatto.identityhub.access.application.MembershipGrantOperation;
import br.dev.andrestamatto.identityhub.access.domain.Membership;
import br.dev.andrestamatto.identityhub.access.domain.MembershipApplicationRef;
import br.dev.andrestamatto.identityhub.access.domain.MembershipId;
import br.dev.andrestamatto.identityhub.access.domain.MembershipUserAccountRef;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class JdbcMembershipProjectionRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-08-03T01:00:00Z");
    private static final UUID APPLICATION =
            UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0");
    private static final UUID CLIENT =
            UUID.fromString("72c43df3-9f34-4dc6-85cc-5d323762f299");
    private static final UUID MEMBERSHIP =
            UUID.fromString("c50638fe-0b91-4f47-81e6-2bd183040c1c");
    private static final UUID USER =
            UUID.fromString("680ac2e4-bfb0-4375-a75e-453b6e7b600c");
    private static final UUID WORKER =
            UUID.fromString("2e993831-1468-44f7-958d-b82a80a784bb");

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17.10"));

    private static JdbcClient jdbc;
    private static JdbcMembershipGrantRepository grants;
    private static JdbcMembershipProjectionRepository projections;

    @BeforeAll
    static void prepare() {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).load().migrate();
        jdbc = JdbcClient.create(dataSource);
        var transactions = new TransactionTemplate(new JdbcTransactionManager(dataSource));
        grants = new JdbcMembershipGrantRepository(jdbc, transactions);
        projections = new JdbcMembershipProjectionRepository(jdbc, transactions);
    }

    @BeforeEach
    void seed() {
        jdbc.sql("delete from membership_projection_outbox").update();
        jdbc.sql("delete from membership_grant_operation").update();
        jdbc.sql("delete from membership").update();
        jdbc.sql("delete from application_client_projection_outbox").update();
        jdbc.sql("delete from application_client").update();
        jdbc.sql("delete from client_application").update();
        jdbc.sql("""
                        insert into client_application
                            (id, identifier, display_name, state, registered_at)
                        values (:id, 'catalog', 'Catalog', 'DRAFT', :now)
                        """)
                .param("id", APPLICATION)
                .param("now", java.time.OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC))
                .update();
        jdbc.sql("""
                        insert into application_client
                            (id, application_id, client_key, client_type, enabled, configured_at)
                        values (:id, :application, 'provisioner', 'MACHINE', true, :now)
                        """)
                .param("id", CLIENT)
                .param("application", APPLICATION)
                .param("now", java.time.OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC))
                .update();
        grants.addOrReplay(operation());
    }

    @Test
    void reservesAndAtomicallyActivatesMembership() {
        var task = projections.reserveNext(WORKER, NOW, Duration.ofSeconds(30)).orElseThrow();

        projections.markApplied(
                task.membership().activate(Clock.fixed(NOW.plusSeconds(1), ZoneOffset.UTC)),
                WORKER,
                NOW.plusSeconds(1));

        assertThat(state("membership")).isEqualTo("ACTIVE");
        assertThat(state("membership_projection_outbox")).isEqualTo("APPLIED");
        var status = grants.findStatus(
                        UUID.fromString("06d068e0-78d5-4df6-97dd-88df538ab948"),
                        new MembershipApplicationRef(APPLICATION))
                .orElseThrow();
        assertThat(status.membershipState()).isEqualTo("ACTIVE");
        assertThat(status.projectionState()).isEqualTo("APPLIED");
        assertThat(status.attempts()).isOne();
        assertThat(grants.findStatus(
                status.operationId(),
                new MembershipApplicationRef(UUID.randomUUID()))).isEmpty();
        assertThat(projections.reserveNext(UUID.randomUUID(), NOW.plusSeconds(31),
                Duration.ofSeconds(30))).isEmpty();
    }

    @Test
    void onlyLeaseOwnerCanCompleteAndExpiredLeaseCanBeReclaimed() {
        projections.reserveNext(WORKER, NOW, Duration.ofSeconds(30)).orElseThrow();

        assertThatThrownBy(() -> projections.markApplied(
                        membership().activate(Clock.fixed(NOW.plusSeconds(1), ZoneOffset.UTC)),
                        UUID.randomUUID(),
                        NOW.plusSeconds(1)))
                .isInstanceOf(IllegalStateException.class);

        assertThat(projections.reserveNext(UUID.randomUUID(), NOW.plusSeconds(31),
                Duration.ofSeconds(30))).isPresent();
    }

    @Test
    void retryReleasesLeaseAndRespectsNextAttempt() {
        var task = projections.reserveNext(WORKER, NOW, Duration.ofSeconds(30)).orElseThrow();

        projections.scheduleRetry(
                task.membership().id(),
                WORKER,
                1,
                NOW.plusSeconds(10),
                "KEYCLOAK_UNAVAILABLE",
                NOW.plusSeconds(1));

        assertThat(projections.reserveNext(UUID.randomUUID(), NOW.plusSeconds(9),
                Duration.ofSeconds(30))).isEmpty();
        assertThat(projections.reserveNext(UUID.randomUUID(), NOW.plusSeconds(10),
                Duration.ofSeconds(30))).isPresent();
    }

    @Test
    void terminalFailureRemainsPendingAndVisible() {
        var task = projections.reserveNext(WORKER, NOW, Duration.ofSeconds(30)).orElseThrow();

        projections.markFailed(
                task.membership().id(),
                WORKER,
                1,
                "USER_NOT_FOUND",
                NOW.plusSeconds(1));

        var status = grants.findStatus(
                        UUID.fromString("06d068e0-78d5-4df6-97dd-88df538ab948"),
                        new MembershipApplicationRef(APPLICATION))
                .orElseThrow();
        assertThat(status.membershipState()).isEqualTo("PENDING");
        assertThat(status.projectionState()).isEqualTo("FAILED");
        assertThat(status.lastFailureCode()).isEqualTo("USER_NOT_FOUND");
    }

    private MembershipGrantOperation operation() {
        return new MembershipGrantOperation(
                UUID.fromString("06d068e0-78d5-4df6-97dd-88df538ab948"),
                membership(),
                CLIENT,
                "membership-grant-001",
                "b9b8c6601d9ce559b9b8c6601d9ce559b9b8c6601d9ce559b9b8c6601d9ce559",
                "membership-projection",
                NOW);
    }

    private Membership membership() {
        return Membership.request(
                new MembershipId(MEMBERSHIP),
                new MembershipApplicationRef(APPLICATION),
                new MembershipUserAccountRef(USER),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private String state(String table) {
        return jdbc.sql("select state from " + table).query(String.class).single();
    }
}
