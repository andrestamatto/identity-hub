package br.dev.andrestamatto.identityhub.identity.domain;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public final class PasswordRecoveryChallenge {

    private static final int MAX_ATTEMPTS = 5;

    private final UUID id;
    private final UserAccountRef userAccountRef;
    private final UUID applicationId;
    private final LoginEmail email;
    private final byte[] secretDigest;
    private final Instant createdAt;
    private final Instant expiresAt;
    private PasswordRecoveryState state;
    private int attempts;
    private Instant usedAt;
    private Instant updatedAt;

    private PasswordRecoveryChallenge(
            UUID id,
            UserAccountRef userAccountRef,
            UUID applicationId,
            LoginEmail email,
            byte[] secretDigest,
            PasswordRecoveryState state,
            int attempts,
            Instant createdAt,
            Instant expiresAt,
            Instant usedAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.userAccountRef = Objects.requireNonNull(userAccountRef);
        this.applicationId = Objects.requireNonNull(applicationId);
        this.email = Objects.requireNonNull(email);
        this.secretDigest = requireDigest(secretDigest);
        this.state = Objects.requireNonNull(state);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.usedAt = usedAt;
        if (!expiresAt.isAfter(createdAt) || attempts < 0 || attempts > MAX_ATTEMPTS) {
            throw new IllegalArgumentException("Password recovery challenge is invalid");
        }
        if ((state == PasswordRecoveryState.USED) != (usedAt != null)
                || (state == PasswordRecoveryState.FAILED && attempts != MAX_ATTEMPTS)) {
            throw new IllegalArgumentException("Password recovery challenge state is invalid");
        }
        this.attempts = attempts;
    }

    public static PasswordRecoveryChallenge start(
            UUID id,
            UserAccountRef userAccountRef,
            UUID applicationId,
            LoginEmail email,
            byte[] secretDigest,
            Instant createdAt,
            Instant expiresAt) {
        return new PasswordRecoveryChallenge(
                id, userAccountRef, applicationId, email, secretDigest,
                PasswordRecoveryState.ACTIVE, 0, createdAt, expiresAt, null, createdAt);
    }

    public static PasswordRecoveryChallenge reconstitute(
            UUID id,
            UserAccountRef userAccountRef,
            UUID applicationId,
            LoginEmail email,
            byte[] secretDigest,
            PasswordRecoveryState state,
            int attempts,
            Instant createdAt,
            Instant expiresAt,
            Instant usedAt,
            Instant updatedAt) {
        return new PasswordRecoveryChallenge(
                id, userAccountRef, applicationId, email, secretDigest, state, attempts,
                createdAt, expiresAt, usedAt, updatedAt);
    }

    public PasswordRecoveryDecision validate(byte[] candidateDigest, Instant now) {
        Objects.requireNonNull(candidateDigest);
        Objects.requireNonNull(now);
        if (state != PasswordRecoveryState.ACTIVE) {
            return PasswordRecoveryDecision.INACTIVE;
        }
        if (!now.isBefore(expiresAt)) {
            state = PasswordRecoveryState.EXPIRED;
            updatedAt = now;
            return PasswordRecoveryDecision.EXPIRED;
        }
        if (MessageDigest.isEqual(secretDigest, candidateDigest)) {
            return PasswordRecoveryDecision.VALID;
        }
        attempts++;
        updatedAt = now;
        if (attempts >= MAX_ATTEMPTS) {
            state = PasswordRecoveryState.FAILED;
        }
        return PasswordRecoveryDecision.INVALID;
    }

    public void markUsed(Instant now) {
        Objects.requireNonNull(now);
        if (state != PasswordRecoveryState.ACTIVE || !now.isBefore(expiresAt)) {
            throw new IllegalStateException("Password recovery challenge is not active");
        }
        state = PasswordRecoveryState.USED;
        usedAt = now;
        updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public UserAccountRef userAccountRef() {
        return userAccountRef;
    }

    public UUID applicationId() {
        return applicationId;
    }

    public LoginEmail email() {
        return email;
    }

    public byte[] secretDigestCopy() {
        return secretDigest.clone();
    }

    public PasswordRecoveryState state() {
        return state;
    }

    public int attempts() {
        return attempts;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant usedAt() {
        return usedAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "PasswordRecoveryChallenge[id=" + id + ", state=" + state
                + ", secret=REDACTED]";
    }

    private static byte[] requireDigest(byte[] value) {
        Objects.requireNonNull(value);
        if (value.length != 32) {
            throw new IllegalArgumentException("Password recovery digest is invalid");
        }
        return Arrays.copyOf(value, value.length);
    }
}
