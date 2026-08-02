package br.dev.andrestamatto.identityhub.identity.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

public final class OnboardingIdentityProof {

    private static final Duration LIFETIME = Duration.ofMinutes(30);

    private final OnboardingDigest digest;
    private final OnboardingSessionId sessionId;
    private final UserAccountRef userAccountRef;
    private final UUID applicationId;
    private final OnboardingDigest acquisitionReferenceDigest;
    private final String correlationId;
    private final boolean emailVerified;
    private final OnboardingIdentityProofState state;
    private final Instant issuedAt;
    private final Instant expiresAt;

    private OnboardingIdentityProof(
            OnboardingDigest digest,
            OnboardingSessionId sessionId,
            UserAccountRef userAccountRef,
            UUID applicationId,
            OnboardingDigest acquisitionReferenceDigest,
            String correlationId,
            boolean emailVerified,
            OnboardingIdentityProofState state,
            Instant issuedAt,
            Instant expiresAt) {
        this.digest = Objects.requireNonNull(digest);
        this.sessionId = Objects.requireNonNull(sessionId);
        this.userAccountRef = Objects.requireNonNull(userAccountRef);
        this.applicationId = Objects.requireNonNull(applicationId);
        this.acquisitionReferenceDigest = Objects.requireNonNull(acquisitionReferenceDigest);
        this.correlationId = requireText(correlationId);
        this.emailVerified = emailVerified;
        this.state = Objects.requireNonNull(state);
        this.issuedAt = Objects.requireNonNull(issuedAt);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        if (!emailVerified || !expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("Onboarding proof invariants are invalid");
        }
    }

    public static OnboardingIdentityProof issue(
            OnboardingDigest digest,
            OnboardingSessionId sessionId,
            UserAccountRef userAccountRef,
            UUID applicationId,
            OnboardingDigest acquisitionReferenceDigest,
            String correlationId,
            Instant now) {
        var issuedAt = Objects.requireNonNull(now).truncatedTo(ChronoUnit.MICROS);
        return new OnboardingIdentityProof(
                digest,
                sessionId,
                userAccountRef,
                applicationId,
                acquisitionReferenceDigest,
                correlationId,
                true,
                OnboardingIdentityProofState.AVAILABLE,
                issuedAt,
                issuedAt.plus(LIFETIME));
    }

    public OnboardingDigest digest() {
        return digest;
    }

    public OnboardingSessionId sessionId() {
        return sessionId;
    }

    public UserAccountRef userAccountRef() {
        return userAccountRef;
    }

    public UUID applicationId() {
        return applicationId;
    }

    public OnboardingDigest acquisitionReferenceDigest() {
        return acquisitionReferenceDigest;
    }

    public String correlationId() {
        return correlationId;
    }

    public boolean emailVerified() {
        return emailVerified;
    }

    public OnboardingIdentityProofState state() {
        return state;
    }

    public Instant issuedAt() {
        return issuedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    @Override
    public String toString() {
        return "OnboardingIdentityProof[state=" + state + "]";
    }

    private static String requireText(String value) {
        Objects.requireNonNull(value);
        if (value.isBlank()) {
            throw new IllegalArgumentException("Correlation id cannot be blank");
        }
        return value;
    }
}
