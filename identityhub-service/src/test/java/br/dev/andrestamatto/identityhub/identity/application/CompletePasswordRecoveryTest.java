package br.dev.andrestamatto.identityhub.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.dev.andrestamatto.identityhub.identity.domain.LoginEmail;
import br.dev.andrestamatto.identityhub.identity.domain.PasswordRecoveryChallenge;
import br.dev.andrestamatto.identityhub.identity.domain.PasswordRecoveryState;
import br.dev.andrestamatto.identityhub.identity.domain.UserAccountRef;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CompletePasswordRecoveryTest {

    private static final UUID APPLICATION_ID =
            UUID.fromString("81618b37-8585-4cf7-9c7b-7a6d06484530");
    private static final UUID ACCOUNT_ID =
            UUID.fromString("85b9dd45-b25f-49e8-b61d-4f14f90c44c0");
    private static final UUID CHALLENGE_ID =
            UUID.fromString("a553de48-f14e-466e-b7a0-86f8477c2530");
    private static final Instant NOW = Instant.parse("2026-08-02T17:00:00Z");
    private static final String SECRET =
            "recovery-proof-with-at-least-256-bits-of-test-entropy";

    @Test
    void consumesProofBeforeResetAndThenRequestsSecurityNotification() {
        var events = new ArrayList<String>();
        var repository = repository(events);
        var useCase = useCase(
                repository,
                (account, email, password) -> events.add("reset"),
                command -> events.add("notify"));

        useCase.execute(command("a new secure password phrase"));

        assertThat(events).containsExactly("consume", "reset", "notify");
        assertThat(repository.challenge.state()).isEqualTo(PasswordRecoveryState.USED);
    }

    @Test
    void rejectsInvalidOrReusedProofWithoutCallingExternalSystems() {
        var events = new ArrayList<String>();
        var repository = repository(events);
        var useCase = useCase(
                repository,
                (account, email, password) -> events.add("reset"),
                command -> events.add("notify"));

        assertThatThrownBy(() -> useCase.execute(new CompletePasswordRecovery.Command(
                        CHALLENGE_ID + ".another-valid-looking-secret",
                        "a new secure password phrase".toCharArray(),
                        "invalid-proof")))
                .isInstanceOf(PasswordRecoveryRejectedException.class);
        assertThat(events).containsExactly("consume");

        repository.challenge = challenge();
        repository.challenge.markUsed(NOW.minusSeconds(1));
        events.clear();
        assertThatThrownBy(() -> useCase.execute(command("a new secure password phrase")))
                .isInstanceOf(PasswordRecoveryRejectedException.class);
        assertThat(events).containsExactly("consume");
    }

    @Test
    void invalidNewPasswordDoesNotConsumeProofAndCommandIsRedacted() {
        var events = new ArrayList<String>();
        var repository = repository(events);
        var command = command("too-short");
        var useCase = useCase(
                repository,
                (account, email, password) -> events.add("reset"),
                ignored -> events.add("notify"));

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(InvalidPasswordRecoveryPasswordException.class);
        assertThat(events).isEmpty();
        assertThat(command.toString()).doesNotContain("too-short", SECRET);
    }

    @Test
    void providerFailureDoesNotReactivateConsumedProofOrSendConfirmation() {
        var events = new ArrayList<String>();
        var repository = repository(events);
        var useCase = useCase(
                repository,
                (account, email, password) -> {
                    events.add("reset");
                    throw new LocalPasswordResetException();
                },
                ignored -> events.add("notify"));

        assertThatThrownBy(() -> useCase.execute(command("a new secure password phrase")))
                .isInstanceOf(LocalPasswordResetException.class);
        assertThat(repository.challenge.state()).isEqualTo(PasswordRecoveryState.USED);
        assertThat(events).containsExactly("consume", "reset");
    }

    private CompletePasswordRecovery useCase(
            CapturingRepository repository,
            LocalPasswordResetter resetter,
            PasswordChangedNotifier notifier) {
        return new CompletePasswordRecovery(
                repository,
                resetter,
                notifier,
                Runnable::run,
                Clock.fixed(NOW, ZoneOffset.UTC),
                () -> UUID.fromString("9091cbcf-372b-4e9a-967c-af94fc2819aa"));
    }

    private CapturingRepository repository(List<String> events) {
        var repository = new CapturingRepository(events);
        repository.challenge = challenge();
        return repository;
    }

    private PasswordRecoveryChallenge challenge() {
        return PasswordRecoveryChallenge.start(
                CHALLENGE_ID,
                new UserAccountRef(ACCOUNT_ID),
                APPLICATION_ID,
                new LoginEmail("andre@example.test"),
                PasswordRecoveryDigest.from(SECRET),
                NOW.minusSeconds(60),
                NOW.plusSeconds(840));
    }

    private CompletePasswordRecovery.Command command(String password) {
        return new CompletePasswordRecovery.Command(
                CHALLENGE_ID + "." + SECRET,
                password.toCharArray(),
                "complete-password-recovery");
    }

    private static final class CapturingRepository
            implements PasswordRecoveryChallengeRepository {
        private final List<String> events;
        private PasswordRecoveryChallenge challenge;

        private CapturingRepository(List<String> events) {
            this.events = events;
        }

        @Override
        public void replaceActive(
                PasswordRecoveryChallenge value,
                Instant windowStart,
                int maximumRequests) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<PasswordRecoveryChallenge> findForUpdate(UUID id) {
            events.add("consume");
            return CHALLENGE_ID.equals(id) ? Optional.of(challenge) : Optional.empty();
        }

        @Override
        public void update(PasswordRecoveryChallenge value) {
            challenge = value;
        }
    }
}
