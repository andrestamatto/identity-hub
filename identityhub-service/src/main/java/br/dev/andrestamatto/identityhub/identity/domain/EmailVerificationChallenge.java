package br.dev.andrestamatto.identityhub.identity.domain;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public final class EmailVerificationChallenge {

    private static final int MAX_ATTEMPTS = 5;

    private final UUID id;
    private final UserAccountRef userAccountRef;
    private final UUID applicationId;
    private final LoginEmail email;
    private final byte[] secretDigest;
    private final Instant createdAt;
    private final Instant expiresAt;
    private EmailVerificationState state;
    private int attempts;
    private Instant usedAt;
    private Instant updatedAt;

    private EmailVerificationChallenge(
            UUID id,
            UserAccountRef userAccountRef,
            UUID applicationId,
            LoginEmail email,
            byte[] secretDigest,
            EmailVerificationState state,
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
            throw new IllegalArgumentException("Email verification challenge is invalid");
        }
        if ((state == EmailVerificationState.USED) != (usedAt != null)
                || (state == EmailVerificationState.FAILED && attempts != MAX_ATTEMPTS)) {
            throw new IllegalArgumentException("Email verification challenge state is invalid");
        }
        this.attempts = attempts;
    }

    public static EmailVerificationChallenge start(
            UUID id,
            UserAccountRef userAccountRef,
            UUID applicationId,
            LoginEmail email,
            byte[] secretDigest,
            Instant createdAt,
            Instant expiresAt) {
        return new EmailVerificationChallenge(
                id,
                userAccountRef,
                applicationId,
                email,
                secretDigest,
                EmailVerificationState.ACTIVE,
                0,
                createdAt,
                expiresAt,
                null,
                createdAt);
    }

    public static EmailVerificationChallenge reconstitute(
            UUID id,
            UserAccountRef userAccountRef,
            UUID applicationId,
            LoginEmail email,
            byte[] secretDigest,
            EmailVerificationState state,
            int attempts,
            Instant createdAt,
            Instant expiresAt,
            Instant usedAt,
            Instant updatedAt) {
        return new EmailVerificationChallenge(
                id, userAccountRef, applicationId, email, secretDigest, state, attempts,
                createdAt, expiresAt, usedAt, updatedAt);
    }

    public EmailVerificationDecision validate(byte[] candidateDigest, Instant now) {
        Objects.requireNonNull(candidateDigest);
        Objects.requireNonNull(now);
        if (state != EmailVerificationState.ACTIVE) {
            return EmailVerificationDecision.INACTIVE;
        }
        if (!now.isBefore(expiresAt)) {
            state = EmailVerificationState.EXPIRED;
            updatedAt = now;
            return EmailVerificationDecision.EXPIRED;
        }
        if (MessageDigest.isEqual(secretDigest, candidateDigest)) {
            return EmailVerificationDecision.VALID;
        }
        attempts++;
        updatedAt = now;
        if (attempts >= MAX_ATTEMPTS) {
            state = EmailVerificationState.FAILED;
        }
        return EmailVerificationDecision.INVALID;
    }

    public void markUsed(Instant now) {
        Objects.requireNonNull(now);
        if (state != EmailVerificationState.ACTIVE || !now.isBefore(expiresAt)) {
            throw new IllegalStateException("Email verification challenge is not active");
        }
        state = EmailVerificationState.USED;
        usedAt = now;
        updatedAt = now;
    }

    public void supersede(Instant now) {
        Objects.requireNonNull(now);
        if (state == EmailVerificationState.ACTIVE) {
            state = EmailVerificationState.SUPERSEDED;
            updatedAt = now;
        }
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

    public EmailVerificationState state() {
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
        return "EmailVerificationChallenge[id=" + id + ", state=" + state
                + ", secret=REDACTED]";
    }

    private static byte[] requireDigest(byte[] value) {
        Objects.requireNonNull(value);
        if (value.length != 32) {
            throw new IllegalArgumentException("Email verification digest is invalid");
        }
        return Arrays.copyOf(value, value.length);
    }
}
