package br.dev.andrestamatto.identityhub.clientapplication.application;

import br.dev.andrestamatto.identityhub.clientapplication.domain.ProtectedApiSettings;
import br.dev.andrestamatto.identityhub.clientapplication.domain.SpaSettings;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ApplicationClientSnapshot(
        UUID applicationClientId,
        UUID applicationId,
        String key,
        String type,
        String audience,
        List<String> redirectUris,
        List<String> webOrigins,
        boolean enabled,
        Instant configuredAt,
        UUID operationId,
        int projectionPayloadVersion,
        String projectionCorrelationId,
        ApplicationClientProjectionState projectionState,
        int projectionAttempts,
        Instant nextProjectionAttemptAt,
        String lastProjectionFailureCode) {

    public ApplicationClientSnapshot {
        redirectUris = List.copyOf(Objects.requireNonNull(redirectUris));
        webOrigins = List.copyOf(Objects.requireNonNull(webOrigins));
    }

    public static ApplicationClientSnapshot from(
            ApplicationClientConfiguration configuration) {
        var client = configuration.client();
        var projection = configuration.projection();
        var audience = client.settings() instanceof ProtectedApiSettings api
                ? api.audience().value()
                : null;
        var redirectUris = client.settings() instanceof SpaSettings spa
                ? spa.redirectUris().stream().map(uri -> uri.value()).toList()
                : List.<String>of();
        var webOrigins = client.settings() instanceof SpaSettings spa
                ? spa.webOrigins().stream().map(origin -> origin.value()).toList()
                : List.<String>of();
        return new ApplicationClientSnapshot(
                client.id().value(),
                client.applicationId().value(),
                client.key().value(),
                client.type().name(),
                audience,
                redirectUris,
                webOrigins,
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
