package br.dev.andrestamatto.identityhub.clientapplication.adapter.out.jdbc;

import br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationConflictException;
import br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationRepository;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationIdentifier;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplication;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplicationId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplicationState;
import br.dev.andrestamatto.identityhub.clientapplication.domain.DisplayName;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;

public final class JdbcClientApplicationRepository implements ClientApplicationRepository {

    private final JdbcClient jdbcClient;

    public JdbcClientApplicationRepository(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient);
    }

    @Override
    public Optional<ClientApplication> findById(ClientApplicationId id) {
        return jdbcClient.sql("""
                        select id, identifier, display_name, state, registered_at
                        from client_application
                        where id = :id
                        """)
                .param("id", id.value())
                .query(this::mapApplication)
                .optional();
    }

    @Override
    public Optional<ClientApplication> findByIdentifier(
            ApplicationIdentifier identifier) {
        return jdbcClient.sql("""
                        select id, identifier, display_name, state, registered_at
                        from client_application
                        where identifier = :identifier
                        """)
                .param("identifier", identifier.value())
                .query(this::mapApplication)
                .optional();
    }

    @Override
    public void add(ClientApplication application) {
        try {
            jdbcClient.sql("""
                            insert into client_application (
                                id,
                                identifier,
                                display_name,
                                state,
                                registered_at
                            ) values (
                                :id,
                                :identifier,
                                :displayName,
                                :state,
                                :registeredAt
                            )
                            """)
                    .param("id", application.id().value())
                    .param("identifier", application.identifier().value())
                    .param("displayName", application.displayName().value())
                    .param("state", application.state().name())
                    .param(
                            "registeredAt",
                            OffsetDateTime.ofInstant(
                                    application.registeredAt(),
                                    ZoneOffset.UTC))
                    .update();
        } catch (DuplicateKeyException exception) {
            throw new ClientApplicationConflictException(
                    "Client application id or identifier is already assigned",
                    exception);
        }
    }

    private ClientApplication mapApplication(ResultSet resultSet, int rowNumber)
            throws SQLException {
        return ClientApplication.reconstitute(
                new ClientApplicationId(resultSet.getObject("id", java.util.UUID.class)),
                new ApplicationIdentifier(resultSet.getString("identifier")),
                new DisplayName(resultSet.getString("display_name")),
                ClientApplicationState.valueOf(resultSet.getString("state")),
                resultSet.getObject("registered_at", OffsetDateTime.class).toInstant());
    }
}
