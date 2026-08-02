package br.dev.andrestamatto.identityhub.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OnboardingSessionTest {

    private static final String SESSION_ID = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final String DIGEST = "a".repeat(64);

    @Test
    void initiatesPendingSessionWithoutHumanIdentity() {
        var createdAt = Instant.parse("2026-08-01T20:00:00Z");
        var session = OnboardingSession.initiate(
                new OnboardingSessionId(SESSION_ID),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new OnboardingDigest(DIGEST),
                "https://app.example.com/auth/callback",
                new PkceCodeChallenge("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"),
                new OnboardingDigest(DIGEST),
                new OnboardingDigest("b".repeat(64)),
                "onboarding-test",
                createdAt);

        assertThat(session.state()).isEqualTo(OnboardingSessionState.PENDING);
        assertThat(session.createdAt()).isEqualTo(createdAt);
        assertThat(session.expiresAt()).isEqualTo(createdAt.plusSeconds(600));
        assertThat(session.toString()).doesNotContain(DIGEST, "callback");
    }

    @Test
    void rejectsInvalidIdentifiersDigestsAndPkce() {
        assertThatThrownBy(() -> new OnboardingSessionId("guessable"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OnboardingDigest("not-a-digest"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PkceCodeChallenge("plain-verifier"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void marksPendingSessionAsProofIssuedBeforeExpiration() {
        var createdAt = Instant.parse("2026-08-01T20:00:00Z");
        var session = pendingSession(createdAt);

        var issuance = session.issueProof(
                new OnboardingDigest("c".repeat(64)),
                new UserAccountRef(UUID.randomUUID()),
                createdAt.plusSeconds(599));
        var completed = issuance.session();

        assertThat(completed.state()).isEqualTo(OnboardingSessionState.PROOF_ISSUED);
        assertThat(completed.proofIssuedAt()).isEqualTo(createdAt.plusSeconds(599));
        assertThat(session.state()).isEqualTo(OnboardingSessionState.PENDING);
    }

    @Test
    void rejectsProofAtExpirationAndAfterPriorIssuance() {
        var createdAt = Instant.parse("2026-08-01T20:00:00Z");
        var session = pendingSession(createdAt);

        assertThatThrownBy(() -> session.issueProof(
                new OnboardingDigest("c".repeat(64)),
                new UserAccountRef(UUID.randomUUID()),
                createdAt.plusSeconds(600)))
                .isInstanceOf(IllegalStateException.class);
        var completed = session.issueProof(
                new OnboardingDigest("c".repeat(64)),
                new UserAccountRef(UUID.randomUUID()),
                createdAt.plusSeconds(599)).session();
        assertThatThrownBy(() -> completed.issueProof(
                new OnboardingDigest("c".repeat(64)),
                new UserAccountRef(UUID.randomUUID()),
                createdAt.plusSeconds(599)))
                .isInstanceOf(IllegalStateException.class);
    }

    private static OnboardingSession pendingSession(Instant createdAt) {
        return OnboardingSession.initiate(
                new OnboardingSessionId(SESSION_ID),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new OnboardingDigest(DIGEST),
                "https://app.example.com/auth/callback",
                new PkceCodeChallenge("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"),
                new OnboardingDigest(DIGEST),
                new OnboardingDigest("b".repeat(64)),
                "onboarding-test",
                createdAt);
    }
}
