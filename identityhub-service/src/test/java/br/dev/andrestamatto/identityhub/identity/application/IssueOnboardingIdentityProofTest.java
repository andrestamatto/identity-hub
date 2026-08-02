package br.dev.andrestamatto.identityhub.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.dev.andrestamatto.identityhub.identity.domain.OnboardingDigest;
import br.dev.andrestamatto.identityhub.identity.domain.OnboardingIdentityProof;
import br.dev.andrestamatto.identityhub.identity.domain.OnboardingProofIssuance;
import br.dev.andrestamatto.identityhub.identity.domain.OnboardingSession;
import br.dev.andrestamatto.identityhub.identity.domain.OnboardingSessionId;
import br.dev.andrestamatto.identityhub.identity.domain.PkceCodeChallenge;
import br.dev.andrestamatto.identityhub.identity.domain.UserAccountRef;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IssueOnboardingIdentityProofTest {

    private static final Instant NOW = Instant.parse("2026-08-02T12:00:00Z");
    private static final String SESSION_ID = "A".repeat(43);
    private static final String RAW_PROOF = "B".repeat(43);

    @Test
    void returnsRawProofOnceAndPersistsOnlyItsDigest() {
        var repository = new StubRepository(pendingSession(NOW.minusSeconds(60)));
        var useCase = useCase(repository);
        var user = new UserAccountRef(UUID.randomUUID());

        var result = useCase.execute(SESSION_ID, new VerifiedOnboardingIdentity(user));

        assertThat(result.proof()).isEqualTo(RAW_PROOF);
        assertThat(result.expiresAt()).isEqualTo(NOW.plusSeconds(1_800));
        assertThat(repository.savedProof.digest().value()).doesNotContain(RAW_PROOF);
        assertThat(repository.savedProof.userAccountRef()).isEqualTo(user);
        assertThat(repository.savedSession.proofIssuedAt()).isEqualTo(NOW);
        assertThat(result.toString()).doesNotContain(RAW_PROOF);
    }

    @Test
    void rejectsMissingExpiredOrAlreadyCompletedSessionWithoutPersistingProof() {
        var missing = new StubRepository(null);
        assertThatThrownBy(() -> useCase(missing).execute(
                SESSION_ID, verifiedIdentity()))
                .isInstanceOf(OnboardingProofRejectedException.class);

        var expired = new StubRepository(pendingSession(NOW.minusSeconds(600)));
        assertThatThrownBy(() -> useCase(expired).execute(
                SESSION_ID, verifiedIdentity()))
                .isInstanceOf(OnboardingProofRejectedException.class);

        var completedSession = pendingSession(NOW.minusSeconds(60)).issueProof(
                new OnboardingDigest("d".repeat(64)),
                new UserAccountRef(UUID.randomUUID()),
                NOW).session();
        var completed = new StubRepository(completedSession);
        assertThatThrownBy(() -> useCase(completed).execute(
                SESSION_ID, verifiedIdentity()))
                .isInstanceOf(OnboardingProofRejectedException.class);

        assertThat(missing.savedProof).isNull();
        assertThat(expired.savedProof).isNull();
        assertThat(completed.savedProof).isNull();
    }

    private static IssueOnboardingIdentityProof useCase(StubRepository repository) {
        return new IssueOnboardingIdentityProof(
                repository,
                Runnable::run,
                Clock.fixed(NOW, ZoneOffset.UTC),
                () -> RAW_PROOF);
    }

    private static VerifiedOnboardingIdentity verifiedIdentity() {
        return new VerifiedOnboardingIdentity(new UserAccountRef(UUID.randomUUID()));
    }

    private static OnboardingSession pendingSession(Instant createdAt) {
        return OnboardingSession.initiate(
                new OnboardingSessionId(SESSION_ID),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new OnboardingDigest("a".repeat(64)),
                "https://app.example.com/auth/callback",
                new PkceCodeChallenge("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"),
                new OnboardingDigest("b".repeat(64)),
                new OnboardingDigest("c".repeat(64)),
                "proof-correlation",
                createdAt);
    }

    private static final class StubRepository implements OnboardingSessionRepository {

        private final OnboardingSession found;
        private OnboardingSession savedSession;
        private OnboardingIdentityProof savedProof;

        private StubRepository(OnboardingSession found) {
            this.found = found;
        }

        @Override
        public SaveResult saveOrFind(OnboardingSession session) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<OnboardingSession> findForUpdate(OnboardingSessionId sessionId) {
            return Optional.ofNullable(found);
        }

        @Override
        public void saveIssuedProof(OnboardingProofIssuance issuance) {
            savedSession = issuance.session();
            savedProof = issuance.proof();
        }
    }
}
