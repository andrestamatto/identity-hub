package br.dev.andrestamatto.identityhub.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PasswordRecoveryChallengeTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-02T16:00:00Z");

    @Test
    void acceptsMatchingDigestOnlyWhileActiveAndUnexpired() {
        var digest = digest(1);
        var challenge = challenge(digest);

        assertThat(challenge.validate(digest, CREATED_AT.plusSeconds(60)))
                .isEqualTo(PasswordRecoveryDecision.VALID);
        challenge.markUsed(CREATED_AT.plusSeconds(60));

        assertThat(challenge.state()).isEqualTo(PasswordRecoveryState.USED);
        assertThat(challenge.validate(digest, CREATED_AT.plusSeconds(61)))
                .isEqualTo(PasswordRecoveryDecision.INACTIVE);
    }

    @Test
    void rejectsAtTheFifteenMinuteBoundaryAndAfterFiveInvalidAttempts() {
        var expired = challenge(digest(1));
        assertThat(expired.validate(digest(1), CREATED_AT.plusSeconds(900)))
                .isEqualTo(PasswordRecoveryDecision.EXPIRED);

        var exhausted = challenge(digest(1));
        for (int attempt = 1; attempt <= 5; attempt++) {
            assertThat(exhausted.validate(digest(attempt + 1), CREATED_AT.plusSeconds(attempt)))
                    .isEqualTo(PasswordRecoveryDecision.INVALID);
        }
        assertThat(exhausted.state()).isEqualTo(PasswordRecoveryState.FAILED);
    }

    private PasswordRecoveryChallenge challenge(byte[] digest) {
        return PasswordRecoveryChallenge.start(
                UUID.fromString("a553de48-f14e-466e-b7a0-86f8477c2530"),
                new UserAccountRef(UUID.fromString("85b9dd45-b25f-49e8-b61d-4f14f90c44c0")),
                UUID.fromString("81618b37-8585-4cf7-9c7b-7a6d06484530"),
                new LoginEmail("andre@example.test"),
                digest,
                CREATED_AT,
                CREATED_AT.plusSeconds(900));
    }

    private byte[] digest(int marker) {
        var digest = new byte[32];
        digest[31] = (byte) marker;
        return digest;
    }
}
