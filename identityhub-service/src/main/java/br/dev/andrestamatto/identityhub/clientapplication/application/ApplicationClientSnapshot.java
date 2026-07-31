package br.dev.andrestamatto.identityhub.clientapplication.application;

import java.time.Instant;
import java.util.UUID;

public record ApplicationClientSnapshot(
        UUID applicationClientId,
        UUID applicationId,
        String key,
        String type,
        String audience,
        boolean enabled,
        Instant configuredAt,
        UUID operationId,
        int projectionPayloadVersion,
        String projectionCorrelationId,
        ApplicationClientProjectionState projectionState,
        int projectionAttempts,
        Instant nextProjectionAttemptAt,
        String lastProjectionFailureCode) {

    public static ApplicationClientSnapshot from(
            ApplicationClientConfiguration configuration) {
        var client = configuration.client();
        var projection = configuration.projection();
        return new ApplicationClientSnapshot(
                client.id().value(),
                client.applicationId().value(),
                client.key().value(),
                client.type().name(),
                client.audience().value(),
                client.enabled(),
                client.configuredAt(),
                projection.operationId(),
                projection.payloadVersion(),
                projection.correlationId(),
                projection.state(),
                projection.attempts(),
                projection.nextAttemptAt(),
                projection.lastFailureCode());
    }
}
