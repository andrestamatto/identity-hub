package br.dev.andrestamatto.identityhub.bootstrap.administration;

import br.dev.andrestamatto.identityhub.bootstrap.config.IdentityHubRuntimeProperties;
import br.dev.andrestamatto.identityhub.bootstrap.security.AdminSecurityProperties;
import java.net.URI;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/admin")
final class AdminRuntimeController {

    private final IdentityHubRuntimeProperties runtimeProperties;
    private final AdminSecurityProperties securityProperties;

    AdminRuntimeController(
            IdentityHubRuntimeProperties runtimeProperties,
            AdminSecurityProperties securityProperties) {
        this.runtimeProperties = runtimeProperties;
        this.securityProperties = securityProperties;
    }

    @GetMapping("/runtime")
    RuntimeInformation runtimeInformation() {
        return new RuntimeInformation(runtimeProperties.environment(), securityProperties.issuerUri());
    }

    record RuntimeInformation(
            IdentityHubRuntimeProperties.DeploymentEnvironment environment,
            URI issuer) {
    }
}
