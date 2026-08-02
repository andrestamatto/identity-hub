package br.dev.andrestamatto.identityhub.identity.adapter.out.jdbc;

import br.dev.andrestamatto.identityhub.identity.application.GlobalAccountDisableOperation;
import br.dev.andrestamatto.identityhub.identity.application.GlobalAccountDisableOperationRepository;
import br.dev.andrestamatto.identityhub.identity.application.GlobalAccountDisableRejection;
import br.dev.andrestamatto.identityhub.identity.application.GlobalAccountDisableStatus;
import br.dev.andrestamatto.identityhub.identity.domain.UserAccountRef;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import org.springframework.jdbc.core.simple.JdbcClient;

public final class JdbcGlobalAccountDisableOperationRepository
        implements GlobalAccountDisableOperationRepository {

    private final JdbcClient jdbcClient;

    public JdbcGlobalAccountDisableOperationRepository(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient);
    }

    @Override
    public GlobalAccountDisableOperation findByIdempotencyKey(String key) {
        return jdbcClient.sql("""
                        select operation_id, user_account_ref, reason, idempotency_key,
                               command_fingerprint, actor_subject, correlation_id, status,
                               rejection, requested_at, completed_at
                        from global_account_disable_operation
                        where idempotency_key = :idempotencyKey
                        """)
                .param("idempotencyKey", key)
                .query(this::map)
                .optional()
                .orElse(null);
    }

    @Override
    public void save(GlobalAccountDisableOperation operation) {
        jdbcClient.sql("""
                        insert into global_account_disable_operation (
                            operation_id, user_account_ref, reason, idempotency_key,
                            command_fingerprint, actor_subject, correlation_id, status,
                            rejection, requested_at, completed_at, updated_at
                        ) values (
                            :operationId, :userAccountRef, :reason, :idempotencyKey,
                            :commandFingerprint, :actorSubject, :correlationId, :status,
                            :rejection, :requestedAt, :completedAt, :updatedAt
                        )
                        on conflict (idempotency_key) do update set
                            status = case
                                when global_account_disable_operation.operation_id = excluded.operation_id
                                    then excluded.status
                                else global_account_disable_operation.status
                            end,
                            rejection = case
                                when global_account_disable_operation.operation_id = excluded.operation_id
                                    then excluded.rejection
                                else global_account_disable_operation.rejection
                            end,
                            completed_at = case
                                when global_account_disable_operation.operation_id = excluded.operation_id
                                    then excluded.completed_at
                                else global_account_disable_operation.completed_at
                            end,
                            updated_at = case
                                when global_account_disable_operation.operation_id = excluded.operation_id
                                    then excluded.updated_at
                                else global_account_disable_operation.updated_at
                            end
                        """)
                .param("operationId", operation.operationId())
                .param("userAccountRef", operation.userAccountRef().value())
                .param("reason", operation.reason())
                .param("idempotencyKey", operation.idempotencyKey())
                .param("commandFingerprint", operation.commandFingerprint())
                .param("actorSubject", operation.actorSubject())
                .param("correlationId", operation.correlationId())
                .param("status", operation.status().name())
                .param("rejection", operation.rejection() == null
                        ? null
                        : operation.rejection().name(), Types.VARCHAR)
                .param("requestedAt", utc(operation.requestedAt()))
                .param("completedAt", operation.completedAt() == null
                        ? null
                        : utc(operation.completedAt()), Types.TIMESTAMP_WITH_TIMEZONE)
                .param("updatedAt", utc(operation.completedAt() == null
                        ? operation.requestedAt()
                        : operation.completedAt()))
                .update();
    }

    @Override
    public void lockGlobalAccountLifecycle() {
        jdbcClient.sql("""
                        select 1
                        from (select pg_advisory_xact_lock(
                            hashtextextended('identityhub-global-account-lifecycle', 0))) locked
                        """)
                .query(Integer.class)
                .single();
    }

    private GlobalAccountDisableOperation map(ResultSet resultSet, int rowNumber)
            throws SQLException {
        var rejection = resultSet.getString("rejection");
        var completedAt = resultSet.getObject("completed_at", OffsetDateTime.class);
        return new GlobalAccountDisableOperation(
                resultSet.getObject("operation_id", java.util.UUID.class),
                new UserAccountRef(resultSet.getObject("user_account_ref", java.util.UUID.class)),
                resultSet.getString("reason"),
                resultSet.getString("idempotency_key"),
                resultSet.getString("command_fingerprint"),
                resultSet.getString("actor_subject"),
                resultSet.getString("correlation_id"),
                GlobalAccountDisableStatus.valueOf(resultSet.getString("status")),
                rejection == null ? null : GlobalAccountDisableRejection.valueOf(rejection),
                resultSet.getObject("requested_at", OffsetDateTime.class).toInstant(),
                completedAt == null ? null : completedAt.toInstant());
    }

    private OffsetDateTime utc(java.time.Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
