package br.dev.andrestamatto.identityhub.access.adapter.out.jdbc;

import br.dev.andrestamatto.identityhub.access.application.MembershipProjectionRepository;
import br.dev.andrestamatto.identityhub.access.application.MembershipProjectionTask;
import br.dev.andrestamatto.identityhub.access.domain.Membership;
import br.dev.andrestamatto.identityhub.access.domain.MembershipApplicationRef;
import br.dev.andrestamatto.identityhub.access.domain.MembershipId;
import br.dev.andrestamatto.identityhub.access.domain.MembershipState;
import br.dev.andrestamatto.identityhub.access.domain.MembershipUserAccountRef;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionOperations;

public final class JdbcMembershipProjectionRepository
        implements MembershipProjectionRepository {

    private final JdbcClient jdbcClient;
    private final TransactionOperations transactions;

    public JdbcMembershipProjectionRepository(
            JdbcClient jdbcClient,
            TransactionOperations transactions) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient);
        this.transactions = Objects.requireNonNull(transactions);
    }

    @Override
    public Optional<MembershipProjectionTask> reserveNext(
            UUID workerId,
            Instant now,
            Duration leaseDuration) {
        return Objects.requireNonNull(transactions.execute(status -> jdbcClient.sql("""
                        with candidate as (
                            select membership_id
                            from membership_projection_outbox
                            where state = 'PENDING'
                              and next_attempt_at <= :now
                              and (lease_until is null or lease_until <= :now)
                            order by next_attempt_at, created_at
                            for update skip locked
                            limit 1
                        )
                        update membership_projection_outbox projection
                        set lease_owner = :workerId,
                            lease_until = :leaseUntil,
                            updated_at = :now
                        from candidate
                        where projection.membership_id = candidate.membership_id
                        returning projection.membership_id
                        """)
                .param("now", utc(now))
                .param("workerId", workerId)
                .param("leaseUntil", utc(now.plus(leaseDuration)))
                .query(UUID.class)
                .optional()
                .flatMap(this::findTask)));
    }

    @Override
    public void markApplied(Membership activeMembership, UUID workerId, Instant now) {
        Objects.requireNonNull(activeMembership);
        if (activeMembership.state() != MembershipState.ACTIVE) {
            throw new IllegalArgumentException("Applied projection requires active membership");
        }
        transactions.executeWithoutResult(status -> {
            var membershipUpdated = jdbcClient.sql("""
                            update membership
                            set state = 'ACTIVE', activated_at = :activatedAt, updated_at = :now
                            where id = :membershipId and state = 'PENDING'
                            """)
                    .param("membershipId", activeMembership.id().value())
                    .param("activatedAt", utc(activeMembership.activatedAt()))
                    .param("now", utc(now))
                    .update();
            var projectionUpdated = updateReserved(
                    activeMembership.id(),
                    workerId,
                    """
                            state = 'APPLIED', attempts = attempts + 1,
                            last_failure_code = null, lease_owner = null,
                            lease_until = null, updated_at = :now
                            """,
                    now,
                    null,
                    null,
                    null);
            if (membershipUpdated != 1 || projectionUpdated != 1) {
                throw new IllegalStateException("Projection is not reserved by this worker");
            }
        });
    }

    @Override
    public void scheduleRetry(
            MembershipId membershipId,
            UUID workerId,
            int attempts,
            Instant nextAttemptAt,
            String failureCode,
            Instant now) {
        ensureUpdated(updateReserved(
                membershipId,
                workerId,
                """
                        attempts = :attempts, next_attempt_at = :nextAttemptAt,
                        last_failure_code = :failureCode, lease_owner = null,
                        lease_until = null, updated_at = :now
                        """,
                now,
                attempts,
                nextAttemptAt,
                failureCode));
    }

    @Override
    public void markFailed(
            MembershipId membershipId,
            UUID workerId,
            int attempts,
            String failureCode,
            Instant now) {
        ensureUpdated(updateReserved(
                membershipId,
                workerId,
                """
                        state = 'FAILED', attempts = :attempts,
                        last_failure_code = :failureCode, lease_owner = null,
                        lease_until = null, updated_at = :now
                        """,
                now,
                attempts,
                null,
                failureCode));
    }

    private Optional<MembershipProjectionTask> findTask(UUID membershipId) {
        return jdbcClient.sql("""
                        select m.id, m.application_id, m.user_account_ref, m.state,
                               m.requested_at, m.activated_at,
                               p.attempts, p.correlation_id
                        from membership m
                        join membership_projection_outbox p on p.membership_id = m.id
                        where m.id = :membershipId
                        """)
                .param("membershipId", membershipId)
                .query(this::mapTask)
                .optional();
    }

    private MembershipProjectionTask mapTask(ResultSet resultSet, int rowNumber)
            throws SQLException {
        var activatedAt = resultSet.getObject("activated_at", OffsetDateTime.class);
        var membership = Membership.reconstitute(
                new MembershipId(resultSet.getObject("id", UUID.class)),
                new MembershipApplicationRef(resultSet.getObject("application_id", UUID.class)),
                new MembershipUserAccountRef(resultSet.getObject("user_account_ref", UUID.class)),
                MembershipState.valueOf(resultSet.getString("state")),
                resultSet.getObject("requested_at", OffsetDateTime.class).toInstant(),
                activatedAt == null ? null : activatedAt.toInstant());
        return new MembershipProjectionTask(
                membership,
                resultSet.getInt("attempts"),
                resultSet.getString("correlation_id"));
    }

    private int updateReserved(
            MembershipId membershipId,
            UUID workerId,
            String assignments,
            Instant now,
            Integer attempts,
            Instant nextAttemptAt,
            String failureCode) {
        var statement = jdbcClient.sql("""
                        update membership_projection_outbox
                        set %s
                        where membership_id = :membershipId
                          and state = 'PENDING'
                          and lease_owner = :workerId
                        """.formatted(assignments))
                .param("membershipId", membershipId.value())
                .param("workerId", workerId)
                .param("now", utc(now));
        if (attempts != null) {
            statement = statement.param("attempts", attempts);
        }
        if (nextAttemptAt != null) {
            statement = statement.param("nextAttemptAt", utc(nextAttemptAt));
        }
        if (failureCode != null) {
            statement = statement.param("failureCode", failureCode);
        }
        return statement.update();
    }

    private static void ensureUpdated(int updated) {
        if (updated != 1) {
            throw new IllegalStateException("Projection is not reserved by this worker");
        }
    }

    private static OffsetDateTime utc(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
