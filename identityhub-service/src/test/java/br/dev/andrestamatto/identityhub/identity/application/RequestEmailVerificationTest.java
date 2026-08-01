package br.dev.andrestamatto.identityhub.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import br.dev.andrestamatto.identityhub.identity.domain.EmailVerificationChallenge;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RequestEmailVerificationTest {

    private static final Instant NOW = Instant.parse("2026-07-31T18:00:00Z");
    private static final UUID CHALLENGE_ID =
            UUID.fromString("27f3aa0b-6a70-43bd-a087-d5bc0c1bc779");

    @Test
    void replacesChallengeAndRequestsDurableOfficialLinkAtomically() {
        var repository = new RecordingRepository();
        var requester = new RecordingEmailRequester();
        var useCase = new RequestEmailVerification(
                repository,
                requester,
                Runnable::run,
                () -> new EmailVerificationSecret("test-only-verification-secret"),
                Clock.fixed(NOW, ZoneOffset.UTC),
                () -> CHALLENGE_ID,
                URI.create("https://auth.dev.example.test"));

        var result = useCase.execute(new RequestEmailVerification.Command(
                UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0"),
                UUID.fromString("9bc4a8c9-405b-4f4a-b443-3c2012369264"),
                "andre@example.test",
                "registration-correlation"));

        assertThat(result.challengeId()).isEqualTo(CHALLENGE_ID);
        assertThat(repository.challenge.expiresAt()).isEqualTo(NOW.plusSeconds(1800));
        assertThat(repository.windowStart).isEqualTo(NOW.minusSeconds(900));
        assertThat(repository.maximumRequests).isEqualTo(3);
        assertThat(requester.verificationUrl).isEqualTo(
                "https://auth.dev.example.test/verify-email#token=" + CHALLENGE_ID
                        + ".test-only-verification-secret");
        assertThat(requester.correlationId).isEqualTo("registration-correlation");
    }

    @Test
    void rejectsInsecureNonLoopbackPublicBaseUri() {
        assertThatIllegalArgumentException().isThrownBy(() -> new RequestEmailVerification(
                new RecordingRepository(),
                new RecordingEmailRequester(),
                Runnable::run,
                () -> new EmailVerificationSecret("test-only-verification-secret"),
                Clock.fixed(NOW, ZoneOffset.UTC),
                () -> CHALLENGE_ID,
                URI.create("http://auth.example.test")));
    }

    private static final class RecordingRepository
            implements EmailVerificationChallengeRepository {
        private EmailVerificationChallenge challenge;
        private Instant windowStart;
        private int maximumRequests;

        @Override
        public void replaceActive(
                EmailVerificationChallenge challenge,
                Instant windowStart,
                int maximumRequests) {
            this.challenge = challenge;
            this.windowStart = windowStart;
            this.maximumRequests = maximumRequests;
        }

        @Override
        public java.util.Optional<EmailVerificationChallenge> findForUpdate(UUID id) {
            return java.util.Optional.empty();
        }

        @Override
        public void update(EmailVerificationChallenge challenge) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class RecordingEmailRequester implements VerificationEmailRequester {
        private String verificationUrl;
        private String correlationId;

        @Override
        public void request(Command command) {
            verificationUrl = command.verificationUrl();
            correlationId = command.correlationId();
        }
    }
}
