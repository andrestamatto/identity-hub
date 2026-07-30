package br.dev.andrestamatto.identityhub.audit.adapter.out.jdbc;

import br.dev.andrestamatto.identityhub.audit.application.AdministrativeAccessEvent;
import br.dev.andrestamatto.identityhub.audit.application.AdministrativeAccessEventRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.simple.JdbcClient;

public final class JdbcAdministrativeAccessEventRepository
        implements AdministrativeAccessEventRepository {

    private final JdbcClient jdbcClient;

    public JdbcAdministrativeAccessEventRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void append(AdministrativeAccessEvent event) {
        jdbcClient.sql("""
                        insert into administrative_access_event (
                            id,
                            occurred_at,
                            correlation_id,
                            actor_subject,
                            http_method,
                            request_path,
                            outcome,
                            reason
                        ) values (
                            :id,
                            :occurredAt,
                            :correlationId,
                            :actorSubject,
                            :method,
                            :path,
                            :outcome,
                            :reason
                        )
                        """)
                .param("id", event.id())
                .param(
                        "occurredAt",
                        OffsetDateTime.ofInstant(event.occurredAt(), ZoneOffset.UTC))
                .param("correlationId", event.correlationId())
                .param("actorSubject", event.actorSubject())
                .param("method", event.method())
                .param("path", event.path())
                .param("outcome", event.outcome().name())
                .param("reason", event.reason())
                .update();
    }
}
