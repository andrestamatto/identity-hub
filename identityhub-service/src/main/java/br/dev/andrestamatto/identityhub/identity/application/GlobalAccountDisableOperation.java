package br.dev.andrestamatto.identityhub.identity.application;

import br.dev.andrestamatto.identityhub.identity.domain.UserAccountRef;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record GlobalAccountDisableOperation(
        UUID operationId,
        UserAccountRef userAccountRef,
        String reason,
        String idempotencyKey,
        String commandFingerprint,
        String actorSubject,
        String correlationId,
        GlobalAccountDisableStatus status,
        GlobalAccountDisableRejection rejection,
        Instant requestedAt,
        Instant completedAt) {

    public GlobalAccountDisableOperation {
        Objects.requireNonNull(operationId);
        Objects.requireNonNull(userAccountRef);
        Objects.requireNonNull(reason);
        Objects.requireNonNull(idempotencyKey);
        Objects.requireNonNull(commandFingerprint);
        Objects.requireNonNull(actorSubject);
        Objects.requireNonNull(correlationId);
        Objects.requireNonNull(status);
        Objects.requireNonNull(requestedAt);
    }

    public GlobalAccountDisableOperation completed(Instant instant) {
        return withStatus(GlobalAccountDisableStatus.COMPLETED, null, instant);
    }

    public GlobalAccountDisableOperation failed(Instant instant) {
        return withStatus(GlobalAccountDisableStatus.FAILED, null, instant);
    }

    public GlobalAccountDisableOperation rejected(
            GlobalAccountDisableRejection rejection,
            Instant instant) {
        return withStatus(GlobalAccountDisableStatus.REJECTED, rejection, instant);
    }

    public GlobalAccountDisableOperation pending() {
        return withStatus(GlobalAccountDisableStatus.PENDING, null, null);
    }

    private GlobalAccountDisableOperation withStatus(
            GlobalAccountDisableStatus nextStatus,
            GlobalAccountDisableRejection nextRejection,
            Instant nextCompletedAt) {
        return new GlobalAccountDisableOperation(
                operationId,
                userAccountRef,
                reason,
                idempotencyKey,
                commandFingerprint,
                actorSubject,
                correlationId,
                nextStatus,
                nextRejection,
                requestedAt,
                nextCompletedAt);
    }
}
