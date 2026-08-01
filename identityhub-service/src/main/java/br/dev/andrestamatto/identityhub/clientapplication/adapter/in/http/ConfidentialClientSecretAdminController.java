package br.dev.andrestamatto.identityhub.clientapplication.adapter.in.http;

import br.dev.andrestamatto.identityhub.clientapplication.application.RotateConfidentialClientSecret;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/admin/client-applications")
@ConditionalOnProperty(
        name = "identityhub.keycloak.management.enabled",
        havingValue = "true")
final class ConfidentialClientSecretAdminController {

    private final RotateConfidentialClientSecret rotateClientSecret;

    ConfidentialClientSecretAdminController(RotateConfidentialClientSecret rotateClientSecret) {
        this.rotateClientSecret = rotateClientSecret;
    }

    @PostMapping("/{applicationId}/clients/{applicationClientId}/credentials/client-secret")
    ResponseEntity<ClientSecretResponse> rotate(
            @PathVariable UUID applicationId,
            @PathVariable UUID applicationClientId) {
        var secret = rotateClientSecret.execute(applicationId, applicationClientId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(new ClientSecretResponse(secret.value()));
    }

    record ClientSecretResponse(String clientSecret) {

        @Override
        public String toString() {
            return "ClientSecretResponse[clientSecret=REDACTED]";
        }
    }
}
