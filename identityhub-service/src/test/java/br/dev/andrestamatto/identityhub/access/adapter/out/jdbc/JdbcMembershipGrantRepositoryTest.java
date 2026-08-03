package br.dev.andrestamatto.identityhub.access.adapter.out.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.dev.andrestamatto.identityhub.access.application.MembershipGrantConflictException;
import br.dev.andrestamatto.identityhub.access.application.MembershipGrantOperation;
import br.dev.andrestamatto.identityhub.access.domain.Membership;
import br.dev.andrestamatto.identityhub.access.domain.MembershipApplicationRef;
import br.dev.andrestamatto.identityhub.access.domain.MembershipId;
import br.dev.andrestamatto.identityhub.access.domain.MembershipUserAccountRef;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
class JdbcMembershipGrantRepositoryTest {

    private static final UUID APPLICATION_ID =
            UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0");
    private static final UUID APPLICATION_CLIENT_ID =
            UUID.fromString("72c43df3-9f34-4dc6-85cc-5d323762f299");
    private static final UUID USER_ACCOUNT_REF =
            UUID.fromString("680ac2e4-bfb0-4375-a75e-453b6e7b600c");
    private static final Instant NOW = Instant.parse("2026-08-02T18:00:00Z");

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17.10"));

    private static JdbcClient jdbcClient;
    private static JdbcMembershipGrantRepository repository;

    @BeforeAll
    static void migrateDatabase() {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).load().migrate();
        jdbcClient = JdbcClient.create(dataSource);
        repository = new JdbcMembershipGrantRepository(
                jdbcClient,
                new TransactionTemplate(new JdbcTransactionManager(dataSource)));
    }

    @BeforeEach
    void resetDatabase() {
        jdbcClient.sql("delete from membership_grant_operation").update();
        jdbcClient.sql("delete from membership").update();
        jdbcClient.sql("delete from application_client_projection_outbox").update();
        jdbcClient.sql("delete from application_client").update();
        jdbcClient.sql("delete from client_application").update();
        jdbcClient.sql("""
                        insert into client_application (
                            id, identifier, display_name, state, registered_at
                        ) values (
                            :id, 'catalog', 'Catalog', 'DRAFT', :registeredAt
                        )
                        """)
                .param("id", APPLICATION_ID)
                .param("registeredAt", java.time.OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC))
                .update();
        jdbcClient.sql("""
                        insert into application_client (
                            id, application_id, client_key, client_type, enabled, configured_at
                        ) values (
                            :id, :applicationId, 'membership-provisioner', 'MACHINE', true,
                            :configuredAt
                        )
                        """)
                .param("id", APPLICATION_CLIENT_ID)
                .param("applicationId", APPLICATION_ID)
                .param("configuredAt", java.time.OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC))
                .update();
    }

    @Test
    void atomicallyReplaysAnIdempotentGrant() {
        var proposed = operation(
                "membership-grant-001",
                UUID.fromString("c50638fe-0b91-4f47-81e6-2bd183040c1c"),
                UUID.fromString("06d068e0-78d5-4df6-97dd-88df538ab948"));

        var first = repository.addOrReplay(proposed);
        var replay = repository.addOrReplay(operation(
                "membership-grant-001", UUID.randomUUID(), UUID.randomUUID()));

        assertThat(replay.operationId()).isEqualTo(first.operationId());
        assertThat(replay.membership().id()).isEqualTo(first.membership().id());
        assertThat(replay.commandFingerprint()).isEqualTo(first.commandFingerprint());
        assertThat(count("membership")).isOne();
        assertThat(count("membership_grant_operation")).isOne();
    }

    @Test
    void differentOperationsReuseTheUniqueMembership() {
        var first = repository.addOrReplay(operation(
                "membership-grant-001", UUID.randomUUID(), UUID.randomUUID()));
        var second = repository.addOrReplay(operation(
                "membership-grant-002", UUID.randomUUID(), UUID.randomUUID()));

        assertThat(second.membership().id()).isEqualTo(first.membership().id());
        assertThat(count("membership")).isOne();
        assertThat(count("membership_grant_operation")).isEqualTo(2);
    }

    @Test
    void conflictingIdempotencyKeyRollsBackTheUnrelatedMembership() {
        repository.addOrReplay(operation(
                "membership-grant-001", UUID.randomUUID(), UUID.randomUUID()));
        var conflicting = operation(
                "membership-grant-001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.fromString("e99f1179-4324-4a4c-b5bc-a7b2151d037d"),
                "a8a7c5500c8bd643a8a7c5500c8bd643a8a7c5500c8bd643a8a7c5500c8bd643");

        assertThatThrownBy(() -> repository.addOrReplay(conflicting))
                .isInstanceOf(MembershipGrantConflictException.class);
        assertThat(count("membership")).isOne();
        assertThat(count("membership_grant_operation")).isOne();
    }

    @Test
    void concurrentOperationsCannotDuplicateTheMembership() throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                ready.countDown();
                start.await();
                return repository.addOrReplay(operation(
                        "membership-grant-001", UUID.randomUUID(), UUID.randomUUID()));
            });
            var second = executor.submit(() -> {
                ready.countDown();
                start.await();
                return repository.addOrReplay(operation(
                        "membership-grant-002", UUID.randomUUID(), UUID.randomUUID()));
            });
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(first.get().membership().id())
                    .isEqualTo(second.get().membership().id());
        }
        assertThat(count("membership")).isOne();
        assertThat(count("membership_grant_operation")).isEqualTo(2);
    }

    private MembershipGrantOperation operation(
            String idempotencyKey,
            UUID membershipId,
            UUID operationId) {
        return operation(
                idempotencyKey,
                membershipId,
                operationId,
                USER_ACCOUNT_REF,
                "b9b8c6601d9ce559b9b8c6601d9ce559b9b8c6601d9ce559b9b8c6601d9ce559");
    }

    private MembershipGrantOperation operation(
            String idempotencyKey,
            UUID membershipId,
            UUID operationId,
            UUID userAccountRef,
            String fingerprint) {
        var membership = Membership.request(
                new MembershipId(membershipId),
                new MembershipApplicationRef(APPLICATION_ID),
                new MembershipUserAccountRef(userAccountRef),
                Clock.fixed(NOW, ZoneOffset.UTC));
        return new MembershipGrantOperation(
                operationId,
                membership,
                APPLICATION_CLIENT_ID,
                idempotencyKey,
                fingerprint,
                "grant-membership",
                NOW);
    }

    private int count(String table) {
        return jdbcClient.sql("select count(*) from " + table)
                .query(Integer.class)
                .single();
    }
}
