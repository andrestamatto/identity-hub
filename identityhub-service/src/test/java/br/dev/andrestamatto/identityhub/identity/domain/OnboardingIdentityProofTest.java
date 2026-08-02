package br.dev.andrestamatto.identityhub.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OnboardingIdentityProofTest {

    @Test
    void issuesEmailVerifiedProofForThirtyMinutesWithoutExposingSensitiveValues() {
        var issuedAt = Instant.parse("2026-08-02T12:00:00Z");
        var proof = OnboardingIdentityProof.issue(
                new OnboardingDigest("a".repeat(64)),
                new OnboardingSessionId("A".repeat(43)),
                new UserAccountRef(UUID.randomUUID()),
                UUID.randomUUID(),
                new OnboardingDigest("b".repeat(64)),
                "proof-correlation",
                issuedAt);

        assertThat(proof.state()).isEqualTo(OnboardingIdentityProofState.AVAILABLE);
        assertThat(proof.emailVerified()).isTrue();
        assertThat(proof.expiresAt()).isEqualTo(issuedAt.plusSeconds(1_800));
        assertThat(proof.toString())
                .doesNotContain(proof.digest().value())
                .doesNotContain(proof.userAccountRef().value().toString())
                .doesNotContain(proof.acquisitionReferenceDigest().value());
    }
}
