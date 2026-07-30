package br.dev.andrestamatto.identityhub.bootstrap.observability;

import static org.assertj.core.api.Assertions.assertThat;

import br.dev.andrestamatto.identityhub.bootstrap.security.AdminSecurityProperties;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;

class KeycloakReadinessHealthIndicatorTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void reportsUpWhenIssuerDiscoveryIsAvailable() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(
                "/realms/identityhub/protocol/openid-connect/certs",
                exchange -> {
                    exchange.sendResponseHeaders(200, -1);
                    exchange.close();
                });
        server.start();
        var issuer = URI.create("http://127.0.0.1:" + server.getAddress().getPort()
                + "/realms/identityhub");
        var properties = new AdminSecurityProperties(
                issuer,
                URI.create(issuer + "/protocol/openid-connect/certs"),
                "identityhub-admin-api");
        var indicator = new KeycloakReadinessHealthIndicator(
                HttpClient.newHttpClient(),
                properties);

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void reportsDownWithoutLeakingRemoteResponseWhenIssuerIsUnavailable() {
        var issuer = URI.create("http://127.0.0.1:1/realms/identityhub");
        var properties = new AdminSecurityProperties(
                issuer,
                URI.create(issuer + "/protocol/openid-connect/certs"),
                "identityhub-admin-api");
        var indicator = new KeycloakReadinessHealthIndicator(
                HttpClient.newHttpClient(),
                properties);

        var health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).isEmpty();
    }
}
