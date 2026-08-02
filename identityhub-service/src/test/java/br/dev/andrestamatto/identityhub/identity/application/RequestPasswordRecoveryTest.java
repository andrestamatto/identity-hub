package br.dev.andrestamatto.identityhub.identity.application;

import static org.assertj.core.api.Assertions.assertThat;

import br.dev.andrestamatto.identityhub.identity.domain.LoginEmail;
import br.dev.andrestamatto.identityhub.identity.domain.PasswordRecoveryChallenge;
import br.dev.andrestamatto.identityhub.identity.domain.UserAccountRef;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RequestPasswordRecoveryTest {

    private static final UUID APPLICATION_ID =
            UUID.fromString("81618b37-8585-4cf7-9c7b-7a6d06484530");
    private static final UUID ACCOUNT_ID =
            UUID.fromString("85b9dd45-b25f-49e8-b61d-4f14f90c44c0");
    private static final UUID CHALLENGE_ID =
            UUID.fromString("a553de48-f14e-466e-b7a0-86f8477c2530");
    private static final Instant NOW = Instant.parse("2026-08-02T16:00:00Z");
    private static final String SECRET =
            "recovery-proof-with-at-least-256-bits-of-test-entropy";

    @Test
    void persistsChallengeAndEmailAtomicallyForEligibleIdentity() {
        var repository = new CapturingRepository();
        var emailRequester = new CapturingEmailRequester();
        var useCase = useCase(
                email -> Optional.of(new PasswordRecoveryIdentity(
                        new UserAccountRef(ACCOUNT_ID),
                        new LoginEmail("Andre@Example.test"))),
                repository,
                emailRequester);

        useCase.execute(new RequestPasswordRecovery.Command(
                APPLICATION_ID, "andre@example.test", "recover-password"));

        assertThat(repository.challenge.userAccountRef().value()).isEqualTo(ACCOUNT_ID);
        assertThat(repository.challenge.applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(repository.challenge.expiresAt()).isEqualTo(NOW.plusSeconds(900));
        assertThat(repository.windowStart).isEqualTo(NOW.minusSeconds(900));
        assertThat(repository.maximumRequests).isEqualTo(3);
        assertThat(repository.challenge.secretDigestCopy())
                .hasSize(32)
                .isNotEqualTo(SECRET.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        assertThat(emailRequester.command.recoveryUrl())
                .isEqualTo("https://auth.example.test/recover-password#token="
                        + CHALLENGE_ID + "." + SECRET);
        assertThat(emailRequester.command.toString()).doesNotContain(SECRET);
    }

    @Test
    void performsNoSensitiveWorkWhenIdentityIsNotEligible() {
        var repository = new CapturingRepository();
        var emailRequester = new CapturingEmailRequester();
        var useCase = useCase(email -> Optional.empty(), repository, emailRequester);

        var command = new RequestPasswordRecovery.Command(
                APPLICATION_ID, "unknown@example.test", "recover-password");
        useCase.execute(command);

        assertThat(repository.challenge).isNull();
        assertThat(emailRequester.command).isNull();
        assertThat(command.toString()).doesNotContain("unknown@example.test");
    }

    private RequestPasswordRecovery useCase(
            PasswordRecoveryIdentityFinder finder,
            CapturingRepository repository,
            CapturingEmailRequester emailRequester) {
        return new RequestPasswordRecovery(
                finder,
                repository,
                emailRequester,
                Runnable::run,
                () -> new PasswordRecoverySecret(SECRET),
                Clock.fixed(NOW, ZoneOffset.UTC),
                () -> CHALLENGE_ID,
                URI.create("https://auth.example.test"));
    }

    private static final class CapturingRepository
            implements PasswordRecoveryChallengeRepository {
        private PasswordRecoveryChallenge challenge;
        private Instant windowStart;
        private int maximumRequests;

        @Override
        public void replaceActive(
                PasswordRecoveryChallenge value,
                Instant windowStart,
                int maximumRequests) {
            this.challenge = value;
            this.windowStart = windowStart;
            this.maximumRequests = maximumRequests;
        }
    }

    private static final class CapturingEmailRequester implements RecoveryEmailRequester {
        private Command command;

        @Override
        public void request(Command value) {
            command = value;
        }
    }
}
