package br.dev.andrestamatto.identityhub.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EmailVerificationChallengeTest {

    private static final Instant CREATED_AT = Instant.parse("2026-07-31T18:00:00Z");

    @Test
    void acceptsMatchingDigestOnlyWhileActiveAndUnexpired() {
        var digest = digest(1);
        var challenge = challenge(digest);

        assertThat(challenge.validate(digest, CREATED_AT.plusSeconds(60)))
                .isEqualTo(EmailVerificationDecision.VALID);
        challenge.markUsed(CREATED_AT.plusSeconds(60));

        assertThat(challenge.state()).isEqualTo(EmailVerificationState.USED);
        assertThat(challenge.validate(digest, CREATED_AT.plusSeconds(61)))
                .isEqualTo(EmailVerificationDecision.INACTIVE);
    }

    @Test
    void closesAfterFiveInvalidAttempts() {
        var challenge = challenge(digest(1));

        for (int attempt = 1; attempt <= 5; attempt++) {
            assertThat(challenge.validate(
                            digest(attempt + 1),
                            CREATED_AT.plusSeconds(attempt)))
                    .isEqualTo(EmailVerificationDecision.INVALID);
        }

        assertThat(challenge.attempts()).isEqualTo(5);
        assertThat(challenge.state()).isEqualTo(EmailVerificationState.FAILED);
    }

    @Test
    void expiresAtTheThirtyMinuteBoundary() {
        var digest = digest(1);
        var challenge = challenge(digest);

        assertThat(challenge.validate(digest, CREATED_AT.plusSeconds(1800)))
                .isEqualTo(EmailVerificationDecision.EXPIRED);
        assertThat(challenge.state()).isEqualTo(EmailVerificationState.EXPIRED);
    }

    private EmailVerificationChallenge challenge(byte[] digest) {
        return EmailVerificationChallenge.start(
                UUID.fromString("27f3aa0b-6a70-43bd-a087-d5bc0c1bc779"),
                new UserAccountRef(UUID.fromString("9bc4a8c9-405b-4f4a-b443-3c2012369264")),
                UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0"),
                new LoginEmail("andre@example.test"),
                digest,
                CREATED_AT,
                CREATED_AT.plusSeconds(1800));
    }

    private byte[] digest(int marker) {
        var digest = new byte[32];
        digest[31] = (byte) marker;
        return digest;
    }
}
