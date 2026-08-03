package br.dev.andrestamatto.identityhub.clientapplication.application;

import br.dev.andrestamatto.identityhub.clientapplication.domain.ProtectedApiSettings;
import br.dev.andrestamatto.identityhub.clientapplication.domain.MachineSettings;
import br.dev.andrestamatto.identityhub.clientapplication.domain.BffSettings;
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
        List<String> scopes,
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
        scopes = List.copyOf(Objects.requireNonNull(scopes));
    }

    public static ApplicationClientSnapshot from(
            ApplicationClientConfiguration configuration) {
        var client = configuration.client();
        var projection = configuration.projection();
        var audience = client.settings() instanceof ProtectedApiSettings api
                ? api.audience().value()
                : null;
        var redirectUris = switch (client.settings()) {
            case SpaSettings spa -> spa.redirectUris().stream()
                    .map(uri -> uri.value())
                    .toList();
            case BffSettings bff -> bff.redirectUris().stream()
                    .map(uri -> uri.value())
                    .toList();
            default -> List.<String>of();
        };
        var webOrigins = client.settings() instanceof SpaSettings spa
                ? spa.webOrigins().stream().map(origin -> origin.value()).toList()
                : List.<String>of();
        var scopes = client.settings() instanceof MachineSettings machine
                ? machine.scopes().stream().map(scope -> scope.value()).toList()
                : List.<String>of();
        return new ApplicationClientSnapshot(
                client.id().value(),
                client.applicationId().value(),
                client.key().value(),
                client.type().name(),
                audience,
                redirectUris,
                webOrigins,
                scopes,
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
