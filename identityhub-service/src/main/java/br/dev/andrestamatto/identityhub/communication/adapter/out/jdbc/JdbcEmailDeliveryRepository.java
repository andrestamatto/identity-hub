package br.dev.andrestamatto.identityhub.communication.adapter.out.jdbc;

import br.dev.andrestamatto.identityhub.communication.application.EmailDelivery;
import br.dev.andrestamatto.identityhub.communication.application.EmailDeliveryConflictException;
import br.dev.andrestamatto.identityhub.communication.application.EmailDeliveryPurpose;
import br.dev.andrestamatto.identityhub.communication.application.EmailDeliveryRepository;
import br.dev.andrestamatto.identityhub.communication.application.EmailDeliveryState;
import br.dev.andrestamatto.identityhub.communication.domain.EmailDeliveryId;
import br.dev.andrestamatto.identityhub.communication.domain.EmailRecipient;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionOperations;

public final class JdbcEmailDeliveryRepository implements EmailDeliveryRepository {

    private static final String SELECT_DELIVERY = """
            select delivery_id, application_id, application_identifier,
                   application_display_name, environment, recipient, purpose, state,
                   sensitive_content,
                   attempts, next_attempt_at, last_failure_code, correlation_id,
                   requested_at, updated_at
            from email_delivery_outbox
            """;

    private final JdbcClient jdbcClient;
    private final TransactionOperations transactions;

    public JdbcEmailDeliveryRepository(
            JdbcClient jdbcClient,
            TransactionOperations transactions) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient);
        this.transactions = Objects.requireNonNull(transactions);
    }

    @Override
    public Optional<EmailDelivery> find(EmailDeliveryId id) {
        return jdbcClient.sql(SELECT_DELIVERY + "where delivery_id = :deliveryId")
                .param("deliveryId", id.value())
                .query(this::mapDelivery)
                .optional();
    }

    @Override
    public void add(EmailDelivery delivery) {
        try {
            jdbcClient.sql("""
                    insert into email_delivery_outbox (
                        delivery_id, application_id, application_identifier,
                        application_display_name, environment, recipient, purpose, state,
                        sensitive_content,
                        attempts, next_attempt_at, last_failure_code, correlation_id,
                        requested_at, updated_at
                    ) values (
                        :deliveryId, :applicationId, :applicationIdentifier,
                        :applicationDisplayName, :environment, :recipient, :purpose, :state,
                        :sensitiveContent,
                        :attempts, :nextAttemptAt, :lastFailureCode, :correlationId,
                        :requestedAt, :updatedAt
                    )
                    """)
                    .param("deliveryId", delivery.id().value())
                    .param("applicationId", delivery.applicationId())
                    .param("applicationIdentifier", delivery.applicationIdentifier())
                    .param("applicationDisplayName", delivery.applicationDisplayName())
                    .param("environment", delivery.environment())
                    .param("recipient", delivery.recipient().value())
                    .param("purpose", delivery.purpose().name())
                    .param("state", delivery.state().name())
                    .param("sensitiveContent", delivery.sensitiveContent(), Types.VARCHAR)
                    .param("attempts", delivery.attempts())
                    .param("nextAttemptAt", utc(delivery.nextAttemptAt()))
                    .param("lastFailureCode", delivery.lastFailureCode(), Types.VARCHAR)
                    .param("correlationId", delivery.correlationId())
                    .param("requestedAt", utc(delivery.requestedAt()))
                    .param("updatedAt", utc(delivery.updatedAt()))
                    .update();
        } catch (DuplicateKeyException exception) {
            throw new EmailDeliveryConflictException();
        }
    }

    @Override
    public Optional<EmailDelivery> reserveNext(
            UUID workerId,
            Instant now,
            Duration lease) {
        return transactions.execute(status -> jdbcClient.sql("""
                        with candidate as (
                            select delivery_id
                            from email_delivery_outbox
                            where state = 'PENDING'
                              and next_attempt_at <= :now
                              and (locked_until is null or locked_until <= :now)
                            order by next_attempt_at, requested_at
                            for update skip locked
                            limit 1
                        )
                        update email_delivery_outbox delivery
                        set locked_by = :workerId,
                            locked_until = :lockedUntil,
                            updated_at = :now
                        from candidate
                        where delivery.delivery_id = candidate.delivery_id
                        returning delivery.delivery_id
                        """)
                .param("now", utc(now))
                .param("workerId", workerId)
                .param("lockedUntil", utc(now.plus(lease)))
                .query(UUID.class)
                .optional()
                .flatMap(id -> find(new EmailDeliveryId(id))));
    }

    @Override
    public void markDelivered(EmailDeliveryId id, UUID workerId, Instant now) {
        updateReserved(
                id, workerId,
                "state = 'DELIVERED', attempts = attempts + 1, "
                        + "last_failure_code = null, locked_by = null, locked_until = null, "
                        + "sensitive_content = null, updated_at = :now",
                null, null, null, now);
    }

    @Override
    public void scheduleRetry(
            EmailDeliveryId id,
            UUID workerId,
            int attempts,
            Instant nextAttemptAt,
            String failureCode,
            Instant now) {
        updateReserved(
                id, workerId,
                "attempts = :attempts, next_attempt_at = :nextAttemptAt, "
                        + "last_failure_code = :failureCode, locked_by = null, "
                        + "locked_until = null, updated_at = :now",
                attempts, nextAttemptAt, failureCode, now);
    }

    @Override
    public void markFailed(
            EmailDeliveryId id,
            UUID workerId,
            int attempts,
            String failureCode,
            Instant now) {
        updateReserved(
                id, workerId,
                "state = 'FAILED', attempts = :attempts, last_failure_code = :failureCode, "
                        + "locked_by = null, locked_until = null, sensitive_content = null, "
                        + "updated_at = :now",
                attempts, null, failureCode, now);
    }

    @Override
    public Optional<EmailDelivery> requeue(EmailDeliveryId id, Instant now) {
        var updated = jdbcClient.sql("""
                        update email_delivery_outbox
                        set state = 'PENDING', attempts = 0, next_attempt_at = :now,
                            last_failure_code = null, locked_by = null, locked_until = null,
                            updated_at = :now
                        where delivery_id = :deliveryId and state = 'FAILED'
                          and (purpose = 'PASSWORD_CHANGED' or sensitive_content is not null)
                        """)
                .param("deliveryId", id.value())
                .param("now", utc(now))
                .update();
        return updated == 0 ? Optional.empty() : find(id);
    }

    private void updateReserved(
            EmailDeliveryId id,
            UUID workerId,
            String assignments,
            Integer attempts,
            Instant nextAttemptAt,
            String failureCode,
            Instant now) {
        var statement = jdbcClient.sql("""
                        update email_delivery_outbox
                        set %s
                        where delivery_id = :deliveryId
                          and state = 'PENDING'
                          and locked_by = :workerId
                        """.formatted(assignments))
                .param("deliveryId", id.value())
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
        if (statement.update() != 1) {
            throw new IllegalStateException("Email delivery is not reserved by this worker");
        }
    }

    private EmailDelivery mapDelivery(ResultSet resultSet, int rowNumber) throws SQLException {
        return EmailDelivery.reconstitute(
                new EmailDeliveryId(resultSet.getObject("delivery_id", UUID.class)),
                resultSet.getObject("application_id", UUID.class),
                resultSet.getString("application_identifier"),
                resultSet.getString("application_display_name"),
                resultSet.getString("environment"),
                new EmailRecipient(resultSet.getString("recipient")),
                EmailDeliveryPurpose.valueOf(resultSet.getString("purpose")),
                resultSet.getString("sensitive_content"),
                EmailDeliveryState.valueOf(resultSet.getString("state")),
                resultSet.getInt("attempts"),
                resultSet.getObject("next_attempt_at", OffsetDateTime.class).toInstant(),
                resultSet.getString("last_failure_code"),
                resultSet.getString("correlation_id"),
                resultSet.getObject("requested_at", OffsetDateTime.class).toInstant(),
                resultSet.getObject("updated_at", OffsetDateTime.class).toInstant());
    }

    private static OffsetDateTime utc(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
