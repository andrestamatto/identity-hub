package br.dev.andrestamatto.identityhub.access.adapter.out.jdbc;

import br.dev.andrestamatto.identityhub.access.application.MembershipGrantConflictException;
import br.dev.andrestamatto.identityhub.access.application.MembershipGrantOperation;
import br.dev.andrestamatto.identityhub.access.application.MembershipGrantRepository;
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
                        select id, application_id, user_account_ref, state, requested_at
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

    private MembershipGrantOperation findOperation(String idempotencyKey) {
        return jdbcClient.sql("""
                        select o.operation_id, o.application_client_id,
                               o.idempotency_key, o.command_fingerprint,
                               o.correlation_id, o.accepted_at,
                               m.id, m.application_id, m.user_account_ref,
                               m.state, m.requested_at
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
                resultSet.getObject("requested_at", OffsetDateTime.class).toInstant());
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
