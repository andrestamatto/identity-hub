package br.dev.andrestamatto.identityhub.access.adapter.out.jdbc;

import br.dev.andrestamatto.identityhub.access.application.MembershipGrantConflictException;
import br.dev.andrestamatto.identityhub.access.application.MembershipGrantOperation;
import br.dev.andrestamatto.identityhub.access.application.MembershipGrantRepository;
import br.dev.andrestamatto.identityhub.access.application.MembershipOperationStatus;
import br.dev.andrestamatto.identityhub.access.domain.Membership;
import br.dev.andrestamatto.identityhub.access.domain.MembershipApplicationRef;
import br.dev.andrestamatto.identityhub.access.domain.MembershipId;
import br.dev.andrestamatto.identityhub.access.domain.MembershipState;
import br.dev.andrestamatto.identityhub.access.domain.MembershipUserAccountRef;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionOperations;

public final class JdbcMembershipGrantRepository implements MembershipGrantRepository {

    private final JdbcClient jdbcClient;
    private final TransactionOperations transactions;

    public JdbcMembershipGrantRepository(
            JdbcClient jdbcClient,
            TransactionOperations transactions) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient);
        this.transactions = Objects.requireNonNull(transactions);
    }

    @Override
    public MembershipGrantOperation addOrReplay(MembershipGrantOperation proposed) {
        Objects.requireNonNull(proposed);
        return Objects.requireNonNull(transactions.execute(status -> addOrReplayAtomically(proposed)));
    }

    private MembershipGrantOperation addOrReplayAtomically(MembershipGrantOperation proposed) {
        var replay = findOperation(proposed.idempotencyKey());
        if (replay != null) {
            ensureEquivalent(replay, proposed);
            return replay;
        }
        insertMembership(proposed.membership());
        var membership = findMembership(
                proposed.membership().applicationRef(),
                proposed.membership().userAccountRef());
        insertProjection(membership, proposed.correlationId());
        insertOperation(proposed.withMembership(membership));
        var stored = Objects.requireNonNull(findOperation(proposed.idempotencyKey()));
        ensureEquivalent(stored, proposed);
        return stored;
    }

    private void insertMembership(Membership membership) {
        jdbcClient.sql("""
                        insert into membership (
                            id, application_id, user_account_ref, state,
                            requested_at, updated_at
                        ) values (
                            :id, :applicationId, :userAccountRef, :state,
                            :requestedAt, :updatedAt
                        )
                        on conflict (application_id, user_account_ref) do nothing
                        """)
                .param("id", membership.id().value())
                .param("applicationId", membership.applicationRef().value())
                .param("userAccountRef", membership.userAccountRef().value())
                .param("state", membership.state().name())
                .param("requestedAt", utc(membership.requestedAt()))
                .param("updatedAt", utc(membership.requestedAt()))
                .update();
    }

    private Membership findMembership(
            MembershipApplicationRef applicationRef,
            MembershipUserAccountRef userAccountRef) {
        return jdbcClient.sql("""
                        select id, application_id, user_account_ref, state,
                               requested_at, activated_at
                        from membership
                        where application_id = :applicationId
                          and user_account_ref = :userAccountRef
                        """)
                .param("applicationId", applicationRef.value())
                .param("userAccountRef", userAccountRef.value())
                .query(this::mapMembership)
                .single();
    }

    private void insertOperation(MembershipGrantOperation operation) {
        jdbcClient.sql("""
                        insert into membership_grant_operation (
                            operation_id, membership_id, application_client_id,
                            idempotency_key, command_fingerprint, correlation_id, accepted_at
                        ) values (
                            :operationId, :membershipId, :applicationClientId,
                            :idempotencyKey, :commandFingerprint, :correlationId, :acceptedAt
                        )
                        on conflict (idempotency_key) do nothing
                        """)
                .param("operationId", operation.operationId())
                .param("membershipId", operation.membership().id().value())
                .param("applicationClientId", operation.applicationClientId())
                .param("idempotencyKey", operation.idempotencyKey())
                .param("commandFingerprint", operation.commandFingerprint())
                .param("correlationId", operation.correlationId())
                .param("acceptedAt", utc(operation.acceptedAt()))
                .update();
    }

    @Override
    public Optional<MembershipOperationStatus> findStatus(
            UUID operationId,
            MembershipApplicationRef applicationRef) {
        Objects.requireNonNull(operationId);
        Objects.requireNonNull(applicationRef);
        return jdbcClient.sql("""
                        select o.operation_id, o.membership_id, m.state membership_state,
                               p.state projection_state, p.attempts, p.last_failure_code,
                               o.accepted_at, p.updated_at
                        from membership_grant_operation o
                        join membership m on m.id = o.membership_id
                        join membership_projection_outbox p on p.membership_id = m.id
                        where o.operation_id = :operationId
                          and m.application_id = :applicationId
                        """)
                .param("operationId", operationId)
                .param("applicationId", applicationRef.value())
                .query((resultSet, rowNumber) -> new MembershipOperationStatus(
                        resultSet.getObject("operation_id", UUID.class),
                        resultSet.getObject("membership_id", UUID.class),
                        resultSet.getString("membership_state"),
                        resultSet.getString("projection_state"),
                        resultSet.getInt("attempts"),
                        resultSet.getString("last_failure_code"),
                        resultSet.getObject("accepted_at", OffsetDateTime.class).toInstant(),
                        resultSet.getObject("updated_at", OffsetDateTime.class).toInstant()))
                .optional();
    }

    @Override
    public Optional<MembershipOperationStatus> requeue(
            UUID operationId,
            MembershipApplicationRef applicationRef,
            java.time.Instant now) {
        return Objects.requireNonNull(transactions.execute(status -> {
            var current = findStatus(operationId, applicationRef);
            if (current.isEmpty() || "PENDING".equals(current.orElseThrow().projectionState())) {
                return current;
            }
            var membershipId = current.orElseThrow().membershipId();
            jdbcClient.sql("""
                            update membership
                            set state = 'PENDING', activated_at = null, updated_at = :now
                            where id = :membershipId
                            """)
                    .param("membershipId", membershipId)
                    .param("now", utc(now))
                    .update();
            var updated = jdbcClient.sql("""
                            update membership_projection_outbox
                            set state = 'PENDING', attempts = 0, next_attempt_at = :now,
                                last_failure_code = null, lease_owner = null,
                                lease_until = null, updated_at = :now
                            where membership_id = :membershipId
                              and lease_owner is null
                            """)
                    .param("membershipId", membershipId)
                    .param("now", utc(now))
                    .update();
            if (updated != 1) {
                throw new IllegalStateException("Membership projection is currently reserved");
            }
            return findStatus(operationId, applicationRef);
        }));
    }

    private void insertProjection(Membership membership, String correlationId) {
        jdbcClient.sql("""
                        insert into membership_projection_outbox (
                            membership_id, payload_version, correlation_id, state,
                            attempts, next_attempt_at, created_at, updated_at
                        )
                        select id, 1, :correlationId, 'PENDING', 0,
                               requested_at, requested_at, updated_at
                        from membership
                        where id = :membershipId
                          and state = 'PENDING'
                        on conflict (membership_id) do nothing
                        """)
                .param("membershipId", membership.id().value())
                .param("correlationId", correlationId)
                .update();
    }

    private MembershipGrantOperation findOperation(String idempotencyKey) {
        return jdbcClient.sql("""
                        select o.operation_id, o.application_client_id,
                               o.idempotency_key, o.command_fingerprint,
                               o.correlation_id, o.accepted_at,
                               m.id, m.application_id, m.user_account_ref,
                               m.state, m.requested_at, m.activated_at
                        from membership_grant_operation o
                        join membership m on m.id = o.membership_id
                        where o.idempotency_key = :idempotencyKey
                        """)
                .param("idempotencyKey", idempotencyKey)
                .query(this::mapOperation)
                .optional()
                .orElse(null);
    }

    private MembershipGrantOperation mapOperation(ResultSet resultSet, int rowNumber)
            throws SQLException {
        return new MembershipGrantOperation(
                resultSet.getObject("operation_id", java.util.UUID.class),
                mapMembership(resultSet, rowNumber),
                resultSet.getObject("application_client_id", java.util.UUID.class),
                resultSet.getString("idempotency_key"),
                resultSet.getString("command_fingerprint"),
                resultSet.getString("correlation_id"),
                resultSet.getObject("accepted_at", OffsetDateTime.class).toInstant());
    }

    private Membership mapMembership(ResultSet resultSet, int rowNumber) throws SQLException {
        return Membership.reconstitute(
                new MembershipId(resultSet.getObject("id", java.util.UUID.class)),
                new MembershipApplicationRef(
                        resultSet.getObject("application_id", java.util.UUID.class)),
                new MembershipUserAccountRef(
                        resultSet.getObject("user_account_ref", java.util.UUID.class)),
                MembershipState.valueOf(resultSet.getString("state")),
                resultSet.getObject("requested_at", OffsetDateTime.class).toInstant(),
                resultSet.getObject("activated_at", OffsetDateTime.class) == null
                        ? null
                        : resultSet.getObject("activated_at", OffsetDateTime.class).toInstant());
    }

    private void ensureEquivalent(
            MembershipGrantOperation stored,
            MembershipGrantOperation proposed) {
        if (!MessageDigest.isEqual(
                stored.commandFingerprint().getBytes(StandardCharsets.US_ASCII),
                proposed.commandFingerprint().getBytes(StandardCharsets.US_ASCII))) {
            throw new MembershipGrantConflictException();
        }
    }

    private static OffsetDateTime utc(java.time.Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
