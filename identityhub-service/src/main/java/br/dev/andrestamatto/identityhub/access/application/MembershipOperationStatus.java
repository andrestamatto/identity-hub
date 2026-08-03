package br.dev.andrestamatto.identityhub.access.application;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record MembershipOperationStatus(
        UUID operationId,
        UUID membershipId,
        String membershipState,
        String projectionState,
        int attempts,
        String lastFailureCode,
        Instant acceptedAt,
        Instant updatedAt) {

    public MembershipOperationStatus {
        Objects.requireNonNull(operationId);
        Objects.requireNonNull(membershipId);
        Objects.requireNonNull(membershipState);
        Objects.requireNonNull(projectionState);
        Objects.requireNonNull(acceptedAt);
        Objects.requireNonNull(updatedAt);
        if (attempts < 0) {
            throw new IllegalArgumentException("Attempts cannot be negative");
        }
    }
}
