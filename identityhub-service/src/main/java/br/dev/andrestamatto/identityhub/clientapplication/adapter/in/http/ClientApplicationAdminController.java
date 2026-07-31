package br.dev.andrestamatto.identityhub.clientapplication.adapter.in.http;

import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientSnapshot;
import br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationSnapshot;
import br.dev.andrestamatto.identityhub.clientapplication.application.ConfigureProtectedApiClient;
import br.dev.andrestamatto.identityhub.clientapplication.application.GetClientApplication;
import br.dev.andrestamatto.identityhub.clientapplication.application.GetApplicationClientConfiguration;
import br.dev.andrestamatto.identityhub.clientapplication.application.ReconcileApplicationClientProjection;
import br.dev.andrestamatto.identityhub.clientapplication.application.RegisterClientApplication;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/admin/client-applications")
final class ClientApplicationAdminController {

    private final RegisterClientApplication registerClientApplication;
    private final GetClientApplication getClientApplication;
    private final ClientApplicationRegistrationMetrics registrationMetrics;
    private final ConfigureProtectedApiClient configureProtectedApiClient;
    private final GetApplicationClientConfiguration getApplicationClient;
    private final ReconcileApplicationClientProjection reconcileProjection;
    private final ApplicationClientManagementMetrics clientMetrics;

    ClientApplicationAdminController(
            RegisterClientApplication registerClientApplication,
            GetClientApplication getClientApplication,
            ClientApplicationRegistrationMetrics registrationMetrics,
            ConfigureProtectedApiClient configureProtectedApiClient,
            GetApplicationClientConfiguration getApplicationClient,
            ReconcileApplicationClientProjection reconcileProjection,
            ApplicationClientManagementMetrics clientMetrics) {
        this.registerClientApplication = registerClientApplication;
        this.getClientApplication = getClientApplication;
        this.registrationMetrics = registrationMetrics;
        this.configureProtectedApiClient = configureProtectedApiClient;
        this.getApplicationClient = getApplicationClient;
        this.reconcileProjection = reconcileProjection;
        this.clientMetrics = clientMetrics;
    }

    @PutMapping("/{applicationId}")
    ResponseEntity<ClientApplicationResponse> register(
            @PathVariable UUID applicationId,
            @RequestBody RegisterClientApplicationRequest request) {
        var command = new RegisterClientApplication.Command(
                applicationId,
                request.identifier(),
                request.displayName());
        var registration = registrationMetrics.record(
                () -> registerClientApplication.execute(command));
        var response = ClientApplicationResponse.from(registration.application());
        if (registration.created()) {
            return ResponseEntity.created(applicationUri(applicationId)).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{applicationId}")
    ClientApplicationResponse get(@PathVariable UUID applicationId) {
        return ClientApplicationResponse.from(getClientApplication.execute(applicationId));
    }

    @PutMapping("/{applicationId}/clients/{applicationClientId}")
    ResponseEntity<ApplicationClientResponse> configureProtectedApi(
            @PathVariable UUID applicationId,
            @PathVariable UUID applicationClientId,
            @RequestBody ConfigureProtectedApiRequest request) {
        var result = clientMetrics.recordConfiguration(() -> configureProtectedApiClient.execute(
                new ConfigureProtectedApiClient.Command(
                        applicationId,
                        applicationClientId,
                        request.key(),
                        request.audience(),
                        MDC.get("correlationId"))));
        var response = ApplicationClientResponse.from(result.client());
        if (result.created()) {
            return ResponseEntity.created(clientUri(applicationId, applicationClientId))
                    .body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{applicationId}/clients/{applicationClientId}")
    ApplicationClientResponse getApplicationClient(
            @PathVariable UUID applicationId,
            @PathVariable UUID applicationClientId) {
        return ApplicationClientResponse.from(
                getApplicationClient.execute(applicationId, applicationClientId));
    }

    @PostMapping("/{applicationId}/clients/{applicationClientId}/projection/reconcile")
    ResponseEntity<ApplicationClientResponse> reconcileProjection(
            @PathVariable UUID applicationId,
            @PathVariable UUID applicationClientId) {
        var snapshot = clientMetrics.recordReconciliation(
                () -> reconcileProjection.execute(applicationId, applicationClientId));
        return ResponseEntity.accepted().body(ApplicationClientResponse.from(snapshot));
    }

    private URI applicationUri(UUID applicationId) {
        return URI.create("/internal/admin/client-applications/" + applicationId);
    }

    private URI clientUri(UUID applicationId, UUID applicationClientId) {
        return URI.create("/internal/admin/client-applications/" + applicationId
                + "/clients/" + applicationClientId);
    }

    record RegisterClientApplicationRequest(String identifier, String displayName) {
    }

    record ConfigureProtectedApiRequest(String key, String audience) {
    }

    record ClientApplicationResponse(
            UUID applicationId,
            String identifier,
            String displayName,
            String state,
            Instant registeredAt) {

        static ClientApplicationResponse from(ClientApplicationSnapshot application) {
            return new ClientApplicationResponse(
                    application.applicationId(),
                    application.identifier(),
                    application.displayName(),
                    application.state().name(),
                    application.registeredAt());
        }
    }

    record ApplicationClientResponse(
            UUID applicationClientId,
            UUID applicationId,
            String key,
            String type,
            String audience,
            boolean enabled,
            Instant configuredAt,
            UUID projectionOperationId,
            int projectionPayloadVersion,
            String projectionCorrelationId,
            String projectionState,
            int projectionAttempts,
            Instant nextProjectionAttemptAt,
            String lastProjectionFailureCode) {

        static ApplicationClientResponse from(ApplicationClientSnapshot client) {
            return new ApplicationClientResponse(
                    client.applicationClientId(),
                    client.applicationId(),
                    client.key(),
                    client.type(),
                    client.audience(),
                    client.enabled(),
                    client.configuredAt(),
                    client.operationId(),
                    client.projectionPayloadVersion(),
                    client.projectionCorrelationId(),
                    client.projectionState().name(),
                    client.projectionAttempts(),
                    client.nextProjectionAttemptAt(),
                    client.lastProjectionFailureCode());
        }
    }
}
