package br.dev.andrestamatto.identityhub.access.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public final class Membership {

    private final MembershipId id;
    private final MembershipApplicationRef applicationRef;
    private final MembershipUserAccountRef userAccountRef;
    private final MembershipState state;
    private final Instant requestedAt;
    private final Instant activatedAt;

    private Membership(
            MembershipId id,
            MembershipApplicationRef applicationRef,
            MembershipUserAccountRef userAccountRef,
            MembershipState state,
            Instant requestedAt,
            Instant activatedAt) {
        this.id = Objects.requireNonNull(id);
        this.applicationRef = Objects.requireNonNull(applicationRef);
        this.userAccountRef = Objects.requireNonNull(userAccountRef);
        this.state = Objects.requireNonNull(state);
        this.requestedAt = Objects.requireNonNull(requestedAt);
        this.activatedAt = activatedAt;
        if (state == MembershipState.PENDING && activatedAt != null) {
            throw new IllegalArgumentException("Pending membership cannot have activation time");
        }
        if (state == MembershipState.ACTIVE && activatedAt == null) {
            throw new IllegalArgumentException("Active membership requires activation time");
        }
    }

    public static Membership request(
            MembershipId id,
            MembershipApplicationRef applicationRef,
            MembershipUserAccountRef userAccountRef,
            Clock clock) {
        Objects.requireNonNull(clock);
        return new Membership(
                id,
                applicationRef,
                userAccountRef,
                MembershipState.PENDING,
                clock.instant().truncatedTo(ChronoUnit.MICROS),
                null);
    }

    public static Membership reconstitute(
            MembershipId id,
            MembershipApplicationRef applicationRef,
            MembershipUserAccountRef userAccountRef,
            MembershipState state,
            Instant requestedAt,
            Instant activatedAt) {
        return new Membership(
                id, applicationRef, userAccountRef, state, requestedAt, activatedAt);
    }

    public Membership activate(Clock clock) {
        Objects.requireNonNull(clock);
        if (state == MembershipState.ACTIVE) {
            return this;
        }
        return new Membership(
                id,
                applicationRef,
                userAccountRef,
                MembershipState.ACTIVE,
                requestedAt,
                clock.instant().truncatedTo(ChronoUnit.MICROS));
    }

    public MembershipId id() {
        return id;
    }

    public MembershipApplicationRef applicationRef() {
        return applicationRef;
    }

    public MembershipUserAccountRef userAccountRef() {
        return userAccountRef;
    }

    public MembershipState state() {
        return state;
    }

    public Instant requestedAt() {
        return requestedAt;
    }

    public Instant activatedAt() {
        return activatedAt;
    }
}
