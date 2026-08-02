package br.dev.andrestamatto.identityhub.identity.domain;

import java.util.Objects;

public record OnboardingProofIssuance(
        OnboardingSession session, OnboardingIdentityProof proof) {

    public OnboardingProofIssuance {
        Objects.requireNonNull(session);
        Objects.requireNonNull(proof);
        if (session.state() != OnboardingSessionState.PROOF_ISSUED
                || !session.id().equals(proof.sessionId())
                || !session.applicationId().equals(proof.applicationId())
                || !session.acquisitionReferenceDigest()
                        .equals(proof.acquisitionReferenceDigest())) {
            throw new IllegalArgumentException("Onboarding proof issuance is inconsistent");
        }
    }
}
