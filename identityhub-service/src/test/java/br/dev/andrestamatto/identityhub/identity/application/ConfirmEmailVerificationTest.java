package br.dev.andrestamatto.identityhub.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.dev.andrestamatto.identityhub.identity.domain.EmailVerificationChallenge;
import br.dev.andrestamatto.identityhub.identity.domain.EmailVerificationState;
import br.dev.andrestamatto.identityhub.identity.domain.LoginEmail;
import br.dev.andrestamatto.identityhub.identity.domain.UserAccountRef;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConfirmEmailVerificationTest {

    private static final Instant NOW = Instant.parse("2026-07-31T18:00:00Z");
    private static final UUID CHALLENGE_ID =
            UUID.fromString("27f3aa0b-6a70-43bd-a087-d5bc0c1bc779");
    private static final String SECRET = "test-only-verification-secret";

    @Test
    void verifiesRemoteIdentityAndConsumesChallengeOnce() {
        var repository = new InMemoryRepository(challenge());
        var verifier = new RecordingVerifier();
        var useCase = useCase(repository, verifier);

        useCase.execute(CHALLENGE_ID + "." + SECRET);

        assertThat(verifier.calls).isOne();
        assertThat(repository.challenge.state()).isEqualTo(EmailVerificationState.USED);
        assertThatThrownBy(() -> useCase.execute(CHALLENGE_ID + "." + SECRET))
                .isInstanceOf(EmailVerificationRejectedException.class);
        assertThat(verifier.calls).isOne();
    }

    @Test
    void persistsInvalidAttemptWithoutCallingProvider() {
        var repository = new InMemoryRepository(challenge());
        var verifier = new RecordingVerifier();

        assertThatThrownBy(() -> useCase(repository, verifier).execute(
                        CHALLENGE_ID + ".another-test-only-secret-value"))
                .isInstanceOf(EmailVerificationRejectedException.class)
                .hasMessage("Email verification could not be completed");

        assertThat(repository.challenge.attempts()).isOne();
        assertThat(repository.updates).isOne();
        assertThat(verifier.calls).isZero();
    }

    private ConfirmEmailVerification useCase(
            InMemoryRepository repository, RecordingVerifier verifier) {
        return new ConfirmEmailVerification(
                repository,
                verifier,
                Runnable::run,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private EmailVerificationChallenge challenge() {
        return EmailVerificationChallenge.start(
                CHALLENGE_ID,
                new UserAccountRef(UUID.fromString("9bc4a8c9-405b-4f4a-b443-3c2012369264")),
                UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0"),
                new LoginEmail("andre@example.test"),
                EmailVerificationDigest.from(SECRET),
                NOW.minusSeconds(60),
                NOW.plusSeconds(1740));
    }

    private static final class InMemoryRepository
            implements EmailVerificationChallengeRepository {
        private final EmailVerificationChallenge challenge;
        private int updates;

        private InMemoryRepository(EmailVerificationChallenge challenge) {
            this.challenge = challenge;
        }

        @Override
        public void replaceActive(
                EmailVerificationChallenge replacement,
                Instant windowStart,
                int maximumRequests) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<EmailVerificationChallenge> findForUpdate(UUID id) {
            return challenge.id().equals(id) ? Optional.of(challenge) : Optional.empty();
        }

        @Override
        public void update(EmailVerificationChallenge changed) {
            updates++;
        }
    }

    private static final class RecordingVerifier implements LocalIdentityVerifier {
        private int calls;

        @Override
        public void verifyAndEnable(UserAccountRef userAccountRef, LoginEmail expectedEmail) {
            calls++;
        }
    }
}
