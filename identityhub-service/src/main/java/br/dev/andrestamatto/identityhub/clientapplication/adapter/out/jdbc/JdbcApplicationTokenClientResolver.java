package br.dev.andrestamatto.identityhub.clientapplication.adapter.out.jdbc;

import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationTokenClient;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationTokenClientResolver;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

public final class JdbcApplicationTokenClientResolver
        implements ApplicationTokenClientResolver {

    private final JdbcClient jdbcClient;

    public JdbcApplicationTokenClientResolver(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient);
    }

    @Override
    public List<ApplicationTokenClient> resolve(UUID applicationId) {
        Objects.requireNonNull(applicationId);
        return jdbcClient.sql("""
                        select c.id, c.client_type, c.audience
                        from application_client c
                        join application_client_projection_outbox o
                          on o.application_client_id = c.id
                        where c.application_id = :applicationId
                          and c.enabled
                          and c.client_type in ('API', 'SPA', 'BFF')
                          and o.state = 'APPLIED'
                        order by c.client_type, c.id
                        """)
                .param("applicationId", applicationId)
                .query((resultSet, rowNumber) -> new ApplicationTokenClient(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("client_type"),
                        resultSet.getString("audience")))
                .list();
    }
}
