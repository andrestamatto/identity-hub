package br.dev.andrestamatto.identityhub.clientapplication.adapter.out.jdbc;

import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientConfiguration;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientConfigurationRepository;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientProjection;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientProjectionState;
import br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationConflictException;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClient;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientKey;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientType;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplicationId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.TokenAudience;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionOperations;

public final class JdbcApplicationClientConfigurationRepository
        implements ApplicationClientConfigurationRepository {

    private static final String SELECT_CONFIGURATION = """
            select
                c.id,
                c.application_id,
                c.client_key,
                c.client_type,
                c.audience,
                c.enabled,
                c.configured_at,
                p.operation_id,
                p.state as projection_state,
                p.attempts,
                p.next_attempt_at,
                p.last_failure_code,
                p.created_at,
                p.updated_at
            from application_client c
            join application_client_projection_outbox p
              on p.application_client_id = c.id
            """;

    private final JdbcClient jdbcClient;
    private final TransactionOperations transactions;

    public JdbcApplicationClientConfigurationRepository(
            JdbcClient jdbcClient,
            TransactionOperations transactions) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient);
        this.transactions = Objects.requireNonNull(transactions);
    }

    @Override
    public Optional<ApplicationClientConfiguration> findById(ApplicationClientId id) {
        return jdbcClient.sql(SELECT_CONFIGURATION + "where c.id = :id")
                .param("id", id.value())
                .query(this::mapConfiguration)
                .optional();
    }

    @Override
    public Optional<ApplicationClientConfiguration> findByKey(
            ClientApplicationId applicationId,
            ApplicationClientKey key) {
        return jdbcClient.sql(SELECT_CONFIGURATION + """
                        where c.application_id = :applicationId
                          and c.client_key = :clientKey
                        """)
                .param("applicationId", applicationId.value())
                .param("clientKey", key.value())
                .query(this::mapConfiguration)
                .optional();
    }

    @Override
    public Optional<ApplicationClientConfiguration> findByAudience(TokenAudience audience) {
        return jdbcClient.sql(SELECT_CONFIGURATION + "where c.audience = :audience")
                .param("audience", audience.value())
                .query(this::mapConfiguration)
                .optional();
    }

    @Override
    public void add(ApplicationClientConfiguration configuration) {
        try {
            transactions.executeWithoutResult(status -> {
                insertClient(configuration.client());
                insertProjection(configuration.projection());
            });
        } catch (DuplicateKeyException exception) {
            throw new ClientApplicationConflictException(
                    "Application client, key, audience, or operation is already assigned",
                    exception);
        }
    }

    private void insertClient(ApplicationClient client) {
        jdbcClient.sql("""
                        insert into application_client (
                            id,
                            application_id,
                            client_key,
                            client_type,
                            audience,
                            enabled,
                            configured_at
                        ) values (
                            :id,
                            :applicationId,
                            :clientKey,
                            :clientType,
                            :audience,
                            :enabled,
                            :configuredAt
                        )
                        """)
                .param("id", client.id().value())
                .param("applicationId", client.applicationId().value())
                .param("clientKey", client.key().value())
                .param("clientType", client.type().name())
                .param("audience", client.audience().value())
                .param("enabled", client.enabled())
                .param(
                        "configuredAt",
                        OffsetDateTime.ofInstant(client.configuredAt(), ZoneOffset.UTC))
                .update();
    }

    private void insertProjection(ApplicationClientProjection projection) {
        jdbcClient.sql("""
                        insert into application_client_projection_outbox (
                            operation_id,
                            application_client_id,
                            state,
                            attempts,
                            next_attempt_at,
                            last_failure_code,
                            created_at,
                            updated_at
                        ) values (
                            :operationId,
                            :clientId,
                            :state,
                            :attempts,
                            :nextAttemptAt,
                            :lastFailureCode,
                            :createdAt,
                            :updatedAt
                        )
                        """)
                .param("operationId", projection.operationId())
                .param("clientId", projection.clientId().value())
                .param("state", projection.state().name())
                .param("attempts", projection.attempts())
                .param(
                        "nextAttemptAt",
                        OffsetDateTime.ofInstant(projection.nextAttemptAt(), ZoneOffset.UTC))
                .param("lastFailureCode", projection.lastFailureCode(), Types.VARCHAR)
                .param(
                        "createdAt",
                        OffsetDateTime.ofInstant(projection.createdAt(), ZoneOffset.UTC))
                .param(
                        "updatedAt",
                        OffsetDateTime.ofInstant(projection.updatedAt(), ZoneOffset.UTC))
                .update();
    }

    private ApplicationClientConfiguration mapConfiguration(
            ResultSet resultSet,
            int rowNumber) throws SQLException {
        var clientId = new ApplicationClientId(resultSet.getObject("id", java.util.UUID.class));
        var client = ApplicationClient.reconstitute(
                clientId,
                new ClientApplicationId(
                        resultSet.getObject("application_id", java.util.UUID.class)),
                new ApplicationClientKey(resultSet.getString("client_key")),
                ApplicationClientType.valueOf(resultSet.getString("client_type")),
                new TokenAudience(resultSet.getString("audience")),
                resultSet.getBoolean("enabled"),
                resultSet.getObject("configured_at", OffsetDateTime.class).toInstant());
        var projection = new ApplicationClientProjection(
                resultSet.getObject("operation_id", java.util.UUID.class),
                clientId,
                ApplicationClientProjectionState.valueOf(
                        resultSet.getString("projection_state")),
                resultSet.getInt("attempts"),
                resultSet.getObject("next_attempt_at", OffsetDateTime.class).toInstant(),
                resultSet.getString("last_failure_code"),
                resultSet.getObject("created_at", OffsetDateTime.class).toInstant(),
                resultSet.getObject("updated_at", OffsetDateTime.class).toInstant());
        return new ApplicationClientConfiguration(client, projection);
    }
}
