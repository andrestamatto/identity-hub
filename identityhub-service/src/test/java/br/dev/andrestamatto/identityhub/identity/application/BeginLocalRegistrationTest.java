package br.dev.andrestamatto.identityhub.identity.application;

import static org.assertj.core.api.Assertions.assertThat;

import br.dev.andrestamatto.identityhub.identity.domain.EmailVerificationChallenge;
import br.dev.andrestamatto.identityhub.identity.domain.UserAccountRef;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BeginLocalRegistrationTest {

    private static final UUID APPLICATION_ID =
            UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0");
    private static final UUID USER_REF =
            UUID.fromString("9bc4a8c9-405b-4f4a-b443-3c2012369264");
    private static final UUID CHALLENGE_ID =
            UUID.fromString("27f3aa0b-6a70-43bd-a087-d5bc0c1bc779");

    @Test
    void pendingRegistrationAlwaysStartsEmailVerification() {
        var challengeRepository = new RecordingChallengeRepository();
        var emailRequester = new RecordingEmailRequester();
        var registerIdentity = new RegisterPendingLocalIdentity(
                id -> true,
                identity -> new LocalIdentityRegistration(
                        new UserAccountRef(USER_REF), true));
        var requestVerification = new RequestEmailVerification(
                challengeRepository,
                emailRequester,
                Runnable::run,
                () -> new EmailVerificationSecret("test-only-verification-secret"),
                Clock.fixed(Instant.parse("2026-07-31T18:00:00Z"), ZoneOffset.UTC),
                () -> CHALLENGE_ID,
                URI.create("https://auth.dev.example.test"));
        var useCase = new BeginLocalRegistration(registerIdentity, requestVerification);

        var result = useCase.execute(new BeginLocalRegistration.Command(
                APPLICATION_ID,
                "andre@example.test",
                "test-only-long-local-password".toCharArray(),
                "begin-registration"));

        assertThat(result.userAccountRef()).isEqualTo(USER_REF);
        assertThat(result.challengeId()).isEqualTo(CHALLENGE_ID);
        assertThat(challengeRepository.challenge.userAccountRef().value()).isEqualTo(USER_REF);
        assertThat(emailRequester.command.recipient()).isEqualTo("andre@example.test");
        assertThat(emailRequester.command.correlationId()).isEqualTo("begin-registration");
    }

    private static final class RecordingChallengeRepository
            implements EmailVerificationChallengeRepository {
        private EmailVerificationChallenge challenge;

        @Override
        public void replaceActive(
                EmailVerificationChallenge replacement,
                Instant windowStart,
                int maximumRequests) {
            challenge = replacement;
        }

        @Override
        public Optional<EmailVerificationChallenge> findForUpdate(UUID id) {
            return Optional.empty();
        }

        @Override
        public void update(EmailVerificationChallenge changed) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class RecordingEmailRequester implements VerificationEmailRequester {
        private Command command;

        @Override
        public void request(Command command) {
            this.command = command;
        }
    }
}
