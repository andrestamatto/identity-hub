package br.dev.andrestamatto.identityhub.identity.adapter.out.jdbc;

import br.dev.andrestamatto.identityhub.identity.application.OnboardingSessionRepository;
import br.dev.andrestamatto.identityhub.identity.domain.OnboardingDigest;
import br.dev.andrestamatto.identityhub.identity.domain.OnboardingSession;
import br.dev.andrestamatto.identityhub.identity.domain.OnboardingSessionId;
import br.dev.andrestamatto.identityhub.identity.domain.OnboardingSessionState;
import br.dev.andrestamatto.identityhub.identity.domain.PkceCodeChallenge;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import org.springframework.jdbc.core.simple.JdbcClient;

public final class JdbcOnboardingSessionRepository implements OnboardingSessionRepository {

    private final JdbcClient jdbcClient;

    public JdbcOnboardingSessionRepository(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient);
    }

    @Override
    public SaveResult saveOrFind(OnboardingSession session) {
        Objects.requireNonNull(session);
        var inserted = jdbcClient.sql("""
                        insert into onboarding_session (
                            id,
                            application_id,
                            machine_client_id,
                            browser_client_id,
                            acquisition_reference_digest,
                            redirect_uri,
                            pkce_code_challenge,
                            idempotency_key_digest,
                            request_digest,
                            correlation_id,
                            state,
                            created_at,
                            expires_at
                        ) values (
                            :id,
                            :applicationId,
                            :machineClientId,
                            :browserClientId,
                            :acquisitionDigest,
                            :redirectUri,
                            :codeChallenge,
                            :idempotencyDigest,
                            :requestDigest,
                            :correlationId,
                            :state,
                            :createdAt,
                            :expiresAt
                        )
                        on conflict (machine_client_id, idempotency_key_digest) do nothing
                        """)
                .param("id", session.id().value())
                .param("applicationId", session.applicationId())
                .param("machineClientId", session.machineClientId())
                .param("browserClientId", session.browserClientId())
                .param("acquisitionDigest", session.acquisitionReferenceDigest().value())
                .param("redirectUri", session.redirectUri())
                .param("codeChallenge", session.codeChallenge().value())
                .param("idempotencyDigest", session.idempotencyKeyDigest().value())
                .param("requestDigest", session.requestDigest().value())
                .param("correlationId", session.correlationId())
                .param("state", session.state().name())
                .param("createdAt", OffsetDateTime.ofInstant(session.createdAt(), ZoneOffset.UTC))
                .param("expiresAt", OffsetDateTime.ofInstant(session.expiresAt(), ZoneOffset.UTC))
                .update();
        if (inserted == 1) {
            return new SaveResult(session, true);
        }
        return new SaveResult(findExisting(session), false);
    }

    private OnboardingSession findExisting(OnboardingSession candidate) {
        return jdbcClient.sql("""
                        select *
                        from onboarding_session
                        where machine_client_id = :machineClientId
                          and idempotency_key_digest = :idempotencyDigest
                        """)
                .param("machineClientId", candidate.machineClientId())
                .param("idempotencyDigest", candidate.idempotencyKeyDigest().value())
                .query(this::mapSession)
                .single();
    }

    private OnboardingSession mapSession(ResultSet resultSet, int rowNumber) throws SQLException {
        return OnboardingSession.reconstitute(
                new OnboardingSessionId(resultSet.getString("id")),
                resultSet.getObject("application_id", java.util.UUID.class),
                resultSet.getObject("machine_client_id", java.util.UUID.class),
                resultSet.getObject("browser_client_id", java.util.UUID.class),
                new OnboardingDigest(resultSet.getString("acquisition_reference_digest")),
                resultSet.getString("redirect_uri"),
                new PkceCodeChallenge(resultSet.getString("pkce_code_challenge")),
                new OnboardingDigest(resultSet.getString("idempotency_key_digest")),
                new OnboardingDigest(resultSet.getString("request_digest")),
                resultSet.getString("correlation_id"),
                OnboardingSessionState.valueOf(resultSet.getString("state")),
                resultSet.getObject("created_at", OffsetDateTime.class).toInstant(),
                resultSet.getObject("expires_at", OffsetDateTime.class).toInstant());
    }
}
