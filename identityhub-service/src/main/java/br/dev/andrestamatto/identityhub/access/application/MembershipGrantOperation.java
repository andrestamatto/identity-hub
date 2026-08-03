package br.dev.andrestamatto.identityhub.access.application;

import br.dev.andrestamatto.identityhub.access.domain.Membership;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record MembershipGrantOperation(
        UUID operationId,
        Membership membership,
        UUID applicationClientId,
        String idempotencyKey,
        String commandFingerprint,
        String correlationId,
        Instant acceptedAt) {

    public MembershipGrantOperation {
        Objects.requireNonNull(operationId);
        Objects.requireNonNull(membership);
        Objects.requireNonNull(applicationClientId);
        Objects.requireNonNull(idempotencyKey);
        Objects.requireNonNull(commandFingerprint);
        Objects.requireNonNull(correlationId);
        Objects.requireNonNull(acceptedAt);
    }

    public MembershipGrantOperation withMembership(Membership storedMembership) {
        return new MembershipGrantOperation(
                operationId,
                storedMembership,
                applicationClientId,
                idempotencyKey,
                commandFingerprint,
                correlationId,
                acceptedAt);
    }
}
