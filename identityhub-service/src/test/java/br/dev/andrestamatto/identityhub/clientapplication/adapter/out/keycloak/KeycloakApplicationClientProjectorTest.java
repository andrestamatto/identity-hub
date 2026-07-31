package br.dev.andrestamatto.identityhub.clientapplication.adapter.out.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientProjectionException;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientProjectionFailureCode;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientProjectionState;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientSnapshot;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

class KeycloakApplicationClientProjectorTest {

    private static final String REALM = "identityhub-test";
    private static final String KEYCLOAK_ID = "keycloak-client-uuid";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final AtomicInteger creates = new AtomicInteger();
    private final AtomicInteger updates = new AtomicInteger();
    private HttpServer server;
    private ObjectNode storedClient;
    private int managementStatus = 200;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/realms/" + REALM + "/protocol/openid-connect/token",
                this::token);
        server.createContext(
                "/admin/realms/" + REALM + "/clients/",
                this::clients);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void createsBearerOnlyApiAndIsIdempotent() {
        var projector = projector();

        projector.project(snapshot());
        projector.project(snapshot());

        assertThat(creates).hasValue(1);
        assertThat(updates).hasValue(0);
        assertThat(storedClient.path("clientId").asString())
                .isEqualTo("ih-api-ff7c4748-f053-4fb6-91be-d34cf0015834");
        assertThat(storedClient.path("bearerOnly").asBoolean()).isTrue();
        assertThat(storedClient.path("standardFlowEnabled").asBoolean()).isFalse();
        assertThat(storedClient.path("directAccessGrantsEnabled").asBoolean()).isFalse();
        assertThat(storedClient.path("serviceAccountsEnabled").asBoolean()).isFalse();
        assertThat(storedClient.path("redirectUris")).isEmpty();
        assertThat(storedClient.path("attributes").path("identityhub.audience").asString())
                .isEqualTo("catalog-api");
    }

    @Test
    void repairsDriftOnlyForOwnedClient() {
        var projector = projector();
        projector.project(snapshot());
        storedClient.put("standardFlowEnabled", true);

        projector.project(snapshot());

        assertThat(updates).hasValue(1);
        assertThat(storedClient.path("standardFlowEnabled").asBoolean()).isFalse();
    }

    @Test
    void refusesToOverwriteClientNotOwnedByIdentityHub() {
        var projector = projector();
        projector.project(snapshot());
        storedClient.withObject("attributes")
                .put("identityhub.application-client-id", UUID.randomUUID().toString());

        assertThatThrownBy(() -> projector.project(snapshot()))
                .isInstanceOf(ApplicationClientProjectionException.class)
                .extracting(exception -> ((ApplicationClientProjectionException) exception)
                        .retryable())
                .isEqualTo(false);
        assertThat(updates).hasValue(0);
    }

    @Test
    void classifiesServerFailureAsRetryableWithoutResponseDetails() {
        managementStatus = 503;

        assertThatThrownBy(() -> projector().project(snapshot()))
                .isInstanceOf(ApplicationClientProjectionException.class)
                .satisfies(exception -> {
                    var projectionException = (ApplicationClientProjectionException) exception;
                    assertThat(projectionException.retryable()).isTrue();
                    assertThat(projectionException.failureCode())
                            .isEqualTo(ApplicationClientProjectionFailureCode.KEYCLOAK_UNAVAILABLE);
                    assertThat(projectionException.getMessage()).doesNotContain("secret");
                });
    }

    private KeycloakApplicationClientProjector projector() {
        return new KeycloakApplicationClientProjector(
                HttpClient.newHttpClient(),
                JSON,
                URI.create("http://127.0.0.1:" + server.getAddress().getPort()),
                REALM,
                "identityhub-management",
                "test-only-secret");
    }

    private ApplicationClientSnapshot snapshot() {
        return new ApplicationClientSnapshot(
                UUID.fromString("ff7c4748-f053-4fb6-91be-d34cf0015834"),
                UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0"),
                "catalog-api",
                "API",
                "catalog-api",
                true,
                Instant.parse("2026-07-31T16:00:00Z"),
                UUID.fromString("27f3aa0b-6a70-43bd-a087-d5bc0c1bc779"),
                ApplicationClientProjectionState.PENDING);
    }

    private void token(HttpExchange exchange) throws IOException {
        respond(exchange, 200, "{\"access_token\":\"synthetic-management-token\"}");
    }

    private void clients(HttpExchange exchange) throws IOException {
        if (managementStatus != 200) {
            respond(exchange, managementStatus, "sensitive upstream details");
            return;
        }
        if ("GET".equals(exchange.getRequestMethod())) {
            var response = storedClient == null ? "[]" : "[" + storedClient + "]";
            respond(exchange, 200, response);
            return;
        }
        var received = (ObjectNode) JSON.readTree(exchange.getRequestBody());
        received.put("id", KEYCLOAK_ID);
        storedClient = received;
        if ("POST".equals(exchange.getRequestMethod())) {
            creates.incrementAndGet();
            respond(exchange, 201, "");
            return;
        }
        if ("PUT".equals(exchange.getRequestMethod())) {
            updates.incrementAndGet();
            respond(exchange, 204, "");
            return;
        }
        respond(exchange, 405, "");
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
