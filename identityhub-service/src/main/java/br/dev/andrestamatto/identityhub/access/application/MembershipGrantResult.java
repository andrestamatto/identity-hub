package br.dev.andrestamatto.identityhub.access.application;

import java.time.Instant;
import java.util.UUID;

public record MembershipGrantResult(
        UUID operationId,
        UUID membershipId,
        UUID applicationId,
        UUID userAccountRef,
        String state,
        Instant acceptedAt) {

    static MembershipGrantResult from(MembershipGrantOperation operation) {
        var membership = operation.membership();
        return new MembershipGrantResult(
                operation.operationId(),
                membership.id().value(),
                membership.applicationRef().value(),
                membership.userAccountRef().value(),
                membership.state().name(),
                operation.acceptedAt());
    }
}
