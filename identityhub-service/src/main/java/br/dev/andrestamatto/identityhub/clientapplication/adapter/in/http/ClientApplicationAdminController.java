package br.dev.andrestamatto.identityhub.clientapplication.adapter.in.http;

import br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationSnapshot;
import br.dev.andrestamatto.identityhub.clientapplication.application.GetClientApplication;
import br.dev.andrestamatto.identityhub.clientapplication.application.RegisterClientApplication;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    ClientApplicationAdminController(
            RegisterClientApplication registerClientApplication,
            GetClientApplication getClientApplication,
            ClientApplicationRegistrationMetrics registrationMetrics) {
        this.registerClientApplication = registerClientApplication;
        this.getClientApplication = getClientApplication;
        this.registrationMetrics = registrationMetrics;
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

    private URI applicationUri(UUID applicationId) {
        return URI.create("/internal/admin/client-applications/" + applicationId);
    }

    record RegisterClientApplicationRequest(String identifier, String displayName) {
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
}
