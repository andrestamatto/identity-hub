package br.dev.andrestamatto.identityhub.identity.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

public final class OnboardingSession {

    private static final Duration LIFETIME = Duration.ofMinutes(10);

    private final OnboardingSessionId id;
    private final UUID applicationId;
    private final UUID machineClientId;
    private final UUID browserClientId;
    private final OnboardingDigest acquisitionReferenceDigest;
    private final String redirectUri;
    private final PkceCodeChallenge codeChallenge;
    private final OnboardingDigest idempotencyKeyDigest;
    private final OnboardingDigest requestDigest;
    private final String correlationId;
    private final OnboardingSessionState state;
    private final Instant createdAt;
    private final Instant expiresAt;

    private OnboardingSession(
            OnboardingSessionId id,
            UUID applicationId,
            UUID machineClientId,
            UUID browserClientId,
            OnboardingDigest acquisitionReferenceDigest,
            String redirectUri,
            PkceCodeChallenge codeChallenge,
            OnboardingDigest idempotencyKeyDigest,
            OnboardingDigest requestDigest,
            String correlationId,
            OnboardingSessionState state,
            Instant createdAt,
            Instant expiresAt) {
        this.id = Objects.requireNonNull(id);
        this.applicationId = Objects.requireNonNull(applicationId);
        this.machineClientId = Objects.requireNonNull(machineClientId);
        this.browserClientId = Objects.requireNonNull(browserClientId);
        this.acquisitionReferenceDigest = Objects.requireNonNull(acquisitionReferenceDigest);
        this.redirectUri = requireText(redirectUri, "Redirect URI");
        this.codeChallenge = Objects.requireNonNull(codeChallenge);
        this.idempotencyKeyDigest = Objects.requireNonNull(idempotencyKeyDigest);
        this.requestDigest = Objects.requireNonNull(requestDigest);
        this.correlationId = requireText(correlationId, "Correlation id");
        this.state = Objects.requireNonNull(state);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("Onboarding session must expire after creation");
        }
    }

    public static OnboardingSession initiate(
            OnboardingSessionId id,
            UUID applicationId,
            UUID machineClientId,
            UUID browserClientId,
            OnboardingDigest acquisitionReferenceDigest,
            String redirectUri,
            PkceCodeChallenge codeChallenge,
            OnboardingDigest idempotencyKeyDigest,
            OnboardingDigest requestDigest,
            String correlationId,
            Instant now) {
        var createdAt = Objects.requireNonNull(now).truncatedTo(ChronoUnit.MICROS);
        return new OnboardingSession(
                id,
                applicationId,
                machineClientId,
                browserClientId,
                acquisitionReferenceDigest,
                redirectUri,
                codeChallenge,
                idempotencyKeyDigest,
                requestDigest,
                correlationId,
                OnboardingSessionState.PENDING,
                createdAt,
                createdAt.plus(LIFETIME));
    }

    public static OnboardingSession reconstitute(
            OnboardingSessionId id,
            UUID applicationId,
            UUID machineClientId,
            UUID browserClientId,
            OnboardingDigest acquisitionReferenceDigest,
            String redirectUri,
            PkceCodeChallenge codeChallenge,
            OnboardingDigest idempotencyKeyDigest,
            OnboardingDigest requestDigest,
            String correlationId,
            OnboardingSessionState state,
            Instant createdAt,
            Instant expiresAt) {
        return new OnboardingSession(
                id,
                applicationId,
                machineClientId,
                browserClientId,
                acquisitionReferenceDigest,
                redirectUri,
                codeChallenge,
                idempotencyKeyDigest,
                requestDigest,
                correlationId,
                state,
                createdAt,
                expiresAt);
    }

    public OnboardingSessionId id() {
        return id;
    }

    public UUID applicationId() {
        return applicationId;
    }

    public UUID machineClientId() {
        return machineClientId;
    }

    public UUID browserClientId() {
        return browserClientId;
    }

    public OnboardingDigest acquisitionReferenceDigest() {
        return acquisitionReferenceDigest;
    }

    public String redirectUri() {
        return redirectUri;
    }

    public PkceCodeChallenge codeChallenge() {
        return codeChallenge;
    }

    public OnboardingDigest idempotencyKeyDigest() {
        return idempotencyKeyDigest;
    }

    public OnboardingDigest requestDigest() {
        return requestDigest;
    }

    public String correlationId() {
        return correlationId;
    }

    public OnboardingSessionState state() {
        return state;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    @Override
    public String toString() {
        return "OnboardingSession[id=[REDACTED], state=" + state + "]";
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value;
    }
}
