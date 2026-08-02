package br.dev.andrestamatto.identityhub.identity.adapter.out.jdbc;

import br.dev.andrestamatto.identityhub.identity.application.PasswordRecoveryChallengeRepository;
import br.dev.andrestamatto.identityhub.identity.application.PasswordRecoveryRateLimitException;
import br.dev.andrestamatto.identityhub.identity.domain.PasswordRecoveryChallenge;
import br.dev.andrestamatto.identityhub.identity.domain.PasswordRecoveryState;
import br.dev.andrestamatto.identityhub.identity.domain.LoginEmail;
import br.dev.andrestamatto.identityhub.identity.domain.UserAccountRef;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

public final class JdbcPasswordRecoveryChallengeRepository
        implements PasswordRecoveryChallengeRepository {

    private static final String SELECT_CHALLENGE = """
            select challenge_id, user_account_ref, application_id, normalized_email,
                   secret_digest, state, attempts, created_at, expires_at, used_at, updated_at
            from password_recovery_challenge
            """;

    private final JdbcClient jdbcClient;

    public JdbcPasswordRecoveryChallengeRepository(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient);
    }

    @Override
    public void replaceActive(
            PasswordRecoveryChallenge challenge,
            Instant windowStart,
            int maximumRequests) {
        Objects.requireNonNull(challenge);
        jdbcClient.sql("""
                        select 1
                        from (select pg_advisory_xact_lock(
                            hashtextextended(:scope, 0))) acquired
                        """)
                .param("scope", challenge.userAccountRef().value().toString())
                .query(Integer.class)
                .single();
        var recent = jdbcClient.sql("""
                        select count(*)
                        from password_recovery_challenge
                        where user_account_ref = :userAccountRef
                          and created_at >= :windowStart
                        """)
                .param("userAccountRef", challenge.userAccountRef().value())
                .param("windowStart", utc(windowStart))
                .query(Long.class)
                .single();
        if (recent >= maximumRequests) {
            throw new PasswordRecoveryRateLimitException();
        }
        jdbcClient.sql("""
                        update password_recovery_challenge
                        set state = 'SUPERSEDED', updated_at = :now
                        where user_account_ref = :userAccountRef
                          and state = 'ACTIVE'
                        """)
                .param("now", utc(challenge.createdAt()))
                .param("userAccountRef", challenge.userAccountRef().value())
                .update();
        jdbcClient.sql("""
                        insert into password_recovery_challenge (
                            challenge_id, user_account_ref, application_id, normalized_email,
                            secret_digest, state, attempts, created_at, expires_at,
                            used_at, updated_at
                        ) values (
                            :challengeId, :userAccountRef, :applicationId, :normalizedEmail,
                            :secretDigest, :state, :attempts, :createdAt, :expiresAt,
                            null, :updatedAt
                        )
                        """)
                .param("challengeId", challenge.id())
                .param("userAccountRef", challenge.userAccountRef().value())
                .param("applicationId", challenge.applicationId())
                .param("normalizedEmail", challenge.email().normalizedValue())
                .param("secretDigest", challenge.secretDigestCopy())
                .param("state", challenge.state().name())
                .param("attempts", challenge.attempts())
                .param("createdAt", utc(challenge.createdAt()))
                .param("expiresAt", utc(challenge.expiresAt()))
                .param("updatedAt", utc(challenge.updatedAt()))
                .update();
    }

    @Override
    public Optional<PasswordRecoveryChallenge> findForUpdate(UUID id) {
        return jdbcClient.sql(SELECT_CHALLENGE
                        + "where challenge_id = :challengeId for update")
                .param("challengeId", id)
                .query(this::mapChallenge)
                .optional();
    }

    @Override
    public void update(PasswordRecoveryChallenge challenge) {
        var updated = jdbcClient.sql("""
                        update password_recovery_challenge
                        set state = :state,
                            attempts = :attempts,
                            used_at = :usedAt,
                            updated_at = :updatedAt
                        where challenge_id = :challengeId
                        """)
                .param("challengeId", challenge.id())
                .param("state", challenge.state().name())
                .param("attempts", challenge.attempts())
                .param("usedAt", challenge.usedAt() == null
                        ? null : utc(challenge.usedAt()), Types.TIMESTAMP_WITH_TIMEZONE)
                .param("updatedAt", utc(challenge.updatedAt()))
                .update();
        if (updated != 1) {
            throw new IllegalStateException("Password recovery challenge was not updated");
        }
    }

    private PasswordRecoveryChallenge mapChallenge(ResultSet resultSet, int rowNumber)
            throws SQLException {
        var usedAt = resultSet.getObject("used_at", OffsetDateTime.class);
        return PasswordRecoveryChallenge.reconstitute(
                resultSet.getObject("challenge_id", UUID.class),
                new UserAccountRef(resultSet.getObject("user_account_ref", UUID.class)),
                resultSet.getObject("application_id", UUID.class),
                new LoginEmail(resultSet.getString("normalized_email")),
                resultSet.getBytes("secret_digest"),
                PasswordRecoveryState.valueOf(resultSet.getString("state")),
                resultSet.getInt("attempts"),
                resultSet.getObject("created_at", OffsetDateTime.class).toInstant(),
                resultSet.getObject("expires_at", OffsetDateTime.class).toInstant(),
                usedAt == null ? null : usedAt.toInstant(),
                resultSet.getObject("updated_at", OffsetDateTime.class).toInstant());
    }

    private static OffsetDateTime utc(Instant value) {
        return OffsetDateTime.ofInstant(value, ZoneOffset.UTC);
    }
}
