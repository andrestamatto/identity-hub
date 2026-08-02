package br.dev.andrestamatto.identityhub.identity.adapter.out.jdbc;

import br.dev.andrestamatto.identityhub.identity.application.OnboardingSessionRepository;
import br.dev.andrestamatto.identityhub.identity.domain.OnboardingDigest;
import br.dev.andrestamatto.identityhub.identity.domain.OnboardingProofIssuance;
import br.dev.andrestamatto.identityhub.identity.domain.OnboardingSession;
import br.dev.andrestamatto.identityhub.identity.domain.OnboardingSessionId;
import br.dev.andrestamatto.identityhub.identity.domain.OnboardingSessionState;
import br.dev.andrestamatto.identityhub.identity.domain.PkceCodeChallenge;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;

public final class JdbcOnboardingSessionRepository implements OnboardingSessionRepository {

    private static final String SELECT_SESSION = """
            select id, application_id, machine_client_id, browser_client_id,
                   acquisition_reference_digest, redirect_uri, pkce_code_challenge,
                   idempotency_key_digest, request_digest, correlation_id, state,
                   created_at, expires_at, proof_issued_at
            from onboarding_session
            """;

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

    @Override
    public Optional<OnboardingSession> findForUpdate(OnboardingSessionId sessionId) {
        Objects.requireNonNull(sessionId);
        return jdbcClient.sql(SELECT_SESSION + "where id = :id for update")
                .param("id", sessionId.value())
                .query(this::mapSession)
                .optional();
    }

    @Override
    public void saveIssuedProof(OnboardingProofIssuance issuance) {
        Objects.requireNonNull(issuance);
        var session = issuance.session();
        var proof = issuance.proof();
        var inserted = jdbcClient.sql("""
                        insert into onboarding_identity_proof (
                            proof_digest,
                            onboarding_session_id,
                            user_account_ref,
                            application_id,
                            acquisition_reference_digest,
                            correlation_id,
                            email_verified,
                            state,
                            issued_at,
                            expires_at
                        ) values (
                            :proofDigest,
                            :sessionId,
                            :userAccountRef,
                            :applicationId,
                            :acquisitionDigest,
                            :correlationId,
                            :emailVerified,
                            :state,
                            :issuedAt,
                            :expiresAt
                        )
                        """)
                .param("proofDigest", proof.digest().value())
                .param("sessionId", proof.sessionId().value())
                .param("userAccountRef", proof.userAccountRef().value())
                .param("applicationId", proof.applicationId())
                .param("acquisitionDigest", proof.acquisitionReferenceDigest().value())
                .param("correlationId", proof.correlationId())
                .param("emailVerified", proof.emailVerified())
                .param("state", proof.state().name())
                .param("issuedAt", utc(proof.issuedAt()))
                .param("expiresAt", utc(proof.expiresAt()))
                .update();
        var updated = jdbcClient.sql("""
                        update onboarding_session
                        set state = :state,
                            proof_issued_at = :proofIssuedAt
                        where id = :id
                          and state = 'PENDING'
                        """)
                .param("state", session.state().name())
                .param("proofIssuedAt", utc(session.proofIssuedAt()))
                .param("id", session.id().value())
                .update();
        if (inserted != 1 || updated != 1) {
            throw new IllegalStateException("Onboarding proof was not persisted");
        }
    }

    private OnboardingSession findExisting(OnboardingSession candidate) {
        return jdbcClient.sql(SELECT_SESSION + """
                        where machine_client_id = :machineClientId
                          and idempotency_key_digest = :idempotencyDigest
                        """)
                .param("machineClientId", candidate.machineClientId())
                .param("idempotencyDigest", candidate.idempotencyKeyDigest().value())
                .query(this::mapSession)
                .single();
    }

    private OnboardingSession mapSession(ResultSet resultSet, int rowNumber) throws SQLException {
        var proofIssuedAt = resultSet.getObject("proof_issued_at", OffsetDateTime.class);
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
                resultSet.getObject("expires_at", OffsetDateTime.class).toInstant(),
                proofIssuedAt == null ? null : proofIssuedAt.toInstant());
    }

    private static OffsetDateTime utc(java.time.Instant value) {
        return OffsetDateTime.ofInstant(value, ZoneOffset.UTC);
    }
}
