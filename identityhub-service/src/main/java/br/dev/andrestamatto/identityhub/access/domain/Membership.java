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

    private Membership(
            MembershipId id,
            MembershipApplicationRef applicationRef,
            MembershipUserAccountRef userAccountRef,
            MembershipState state,
            Instant requestedAt) {
        this.id = Objects.requireNonNull(id);
        this.applicationRef = Objects.requireNonNull(applicationRef);
        this.userAccountRef = Objects.requireNonNull(userAccountRef);
        this.state = Objects.requireNonNull(state);
        this.requestedAt = Objects.requireNonNull(requestedAt);
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
                clock.instant().truncatedTo(ChronoUnit.MICROS));
    }

    public static Membership reconstitute(
            MembershipId id,
            MembershipApplicationRef applicationRef,
            MembershipUserAccountRef userAccountRef,
            MembershipState state,
            Instant requestedAt) {
        return new Membership(id, applicationRef, userAccountRef, state, requestedAt);
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
}
