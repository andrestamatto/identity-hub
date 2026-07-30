package br.dev.andrestamatto.identityhub.bootstrap.observability;

import br.dev.andrestamatto.identityhub.bootstrap.security.AdminSecurityProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("keycloak")
final class KeycloakReadinessHealthIndicator implements HealthIndicator {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(2);

    private final HttpClient httpClient;
    private final URI jwkSetUri;

    KeycloakReadinessHealthIndicator(
            HttpClient httpClient,
            AdminSecurityProperties properties) {
        this.httpClient = httpClient;
        this.jwkSetUri = properties.jwkSetUri();
    }

    @Override
    public Health health() {
        var request = HttpRequest.newBuilder(jwkSetUri)
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() >= 200 && response.statusCode() < 300
                    ? Health.up().build()
                    : Health.down().build();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Health.down().build();
        } catch (IOException | RuntimeException exception) {
            return Health.down().build();
        }
    }

}
