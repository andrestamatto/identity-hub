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
    private static final String MEMBERSHIP_SCOPE_ID = "membership-scope-uuid";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final AtomicInteger creates = new AtomicInteger();
    private final AtomicInteger updates = new AtomicInteger();
    private final AtomicInteger rotations = new AtomicInteger();
    private final AtomicInteger clientScopeCreates = new AtomicInteger();
    private final AtomicInteger defaultScopeAttachments = new AtomicInteger();
    private final AtomicInteger defaultScopeRemovals = new AtomicInteger();
    private HttpServer server;
    private ObjectNode storedClient;
    private ObjectNode storedClientScope;
    private boolean membershipScopeAttached;
    private boolean profileScopeAttached = true;
    private int managementStatus = 200;
    private boolean failAfterCreate;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/realms/" + REALM + "/protocol/openid-connect/token",
                this::token);
        server.createContext(
                "/admin/realms/" + REALM + "/clients/",
                this::clients);
        server.createContext(
                "/admin/realms/" + REALM + "/client-scopes",
                this::clientScopes);
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

    @Test
    void retryAfterLostCreateResponseFindsAppliedRemoteEffect() {
        var projector = projector();
        failAfterCreate = true;

        assertThatThrownBy(() -> projector.project(snapshot()))
                .isInstanceOf(ApplicationClientProjectionException.class)
                .satisfies(exception -> assertThat(
                                ((ApplicationClientProjectionException) exception).retryable())
                        .isTrue());

        failAfterCreate = false;
        projector.project(snapshot());

        assertThat(creates).hasValue(1);
        assertThat(updates).hasValue(0);
    }

    @Test
    void createsPublicSpaWithAuthorizationCodeAndPkceS256() {
        projector().project(spaSnapshot());

        assertThat(storedClient.path("clientId").asString())
                .isEqualTo("ih-spa-ff7c4748-f053-4fb6-91be-d34cf0015834");
        assertThat(storedClient.path("publicClient").asBoolean()).isTrue();
        assertThat(storedClient.path("bearerOnly").asBoolean()).isFalse();
        assertThat(storedClient.path("standardFlowEnabled").asBoolean()).isTrue();
        assertThat(storedClient.path("implicitFlowEnabled").asBoolean()).isFalse();
        assertThat(storedClient.path("directAccessGrantsEnabled").asBoolean()).isFalse();
        assertThat(storedClient.path("serviceAccountsEnabled").asBoolean()).isFalse();
        assertThat(storedClient.path("redirectUris"))
                .containsExactly(JSON.valueToTree("https://app.example.com/auth/callback"));
        assertThat(storedClient.path("webOrigins"))
                .containsExactly(JSON.valueToTree("https://app.example.com"));
        assertThat(storedClient.path("attributes").path("pkce.code.challenge.method").asString())
                .isEqualTo("S256");
        assertThat(storedClient.path("secret").isMissingNode()).isTrue();
    }

    @Test
    void createsConfidentialBffWithAuthorizationCodeAndPkceS256() {
        projector().project(bffSnapshot());

        assertThat(storedClient.path("clientId").asString())
                .isEqualTo("ih-bff-72c43df3-9f34-4dc6-85cc-5d323762f299");
        assertThat(storedClient.path("publicClient").asBoolean()).isFalse();
        assertThat(storedClient.path("bearerOnly").asBoolean()).isFalse();
        assertThat(storedClient.path("clientAuthenticatorType").asString())
                .isEqualTo("client-secret");
        assertThat(storedClient.path("standardFlowEnabled").asBoolean()).isTrue();
        assertThat(storedClient.path("implicitFlowEnabled").asBoolean()).isFalse();
        assertThat(storedClient.path("directAccessGrantsEnabled").asBoolean()).isFalse();
        assertThat(storedClient.path("serviceAccountsEnabled").asBoolean()).isFalse();
        assertThat(storedClient.path("redirectUris"))
                .containsExactly(JSON.valueToTree(
                        "https://app.example.com/login/oauth2/code/identityhub"));
        assertThat(storedClient.path("webOrigins")).isEmpty();
        assertThat(storedClient.path("attributes").path("pkce.code.challenge.method").asString())
                .isEqualTo("S256");
        assertThat(storedClient.path("secret").isMissingNode()).isTrue();
    }

    @Test
    void createsConfidentialMachineWithOnlyServiceAccountsEnabled() {
        var projector = projector();

        projector.project(machineSnapshot());
        projector.project(machineSnapshot());

        assertThat(storedClient.path("clientId").asString())
                .isEqualTo("ih-machine-72c43df3-9f34-4dc6-85cc-5d323762f299");
        assertThat(storedClient.path("publicClient").asBoolean()).isFalse();
        assertThat(storedClient.path("bearerOnly").asBoolean()).isFalse();
        assertThat(storedClient.path("clientAuthenticatorType").asString())
                .isEqualTo("client-secret");
        assertThat(storedClient.path("standardFlowEnabled").asBoolean()).isFalse();
        assertThat(storedClient.path("implicitFlowEnabled").asBoolean()).isFalse();
        assertThat(storedClient.path("directAccessGrantsEnabled").asBoolean()).isFalse();
        assertThat(storedClient.path("serviceAccountsEnabled").asBoolean()).isTrue();
        assertThat(storedClient.path("authorizationServicesEnabled").asBoolean()).isFalse();
        assertThat(storedClient.path("fullScopeAllowed").asBoolean()).isFalse();
        assertThat(storedClient.path("redirectUris")).isEmpty();
        assertThat(storedClient.path("webOrigins")).isEmpty();
        assertThat(storedClient.path("attributes")
                .path("pkce.code.challenge.method").isMissingNode()).isTrue();
        assertThat(storedClient.path("secret").isMissingNode()).isTrue();
        assertThat(clientScopeCreates).hasValue(1);
        assertThat(defaultScopeAttachments).hasValue(1);
        assertThat(defaultScopeRemovals).hasValue(1);
        assertThat(storedClientScope.path("name").asString())
                .isEqualTo("membership:write");
        assertThat(storedClientScope.path("protocolMappers").get(0)
                .path("config").path("included.custom.audience").asString())
                .isEqualTo("identityhub-integration-api");
    }

    @Test
    void refusesToOverwriteMembershipScopeNotOwnedByIdentityHub() {
        storedClientScope = JSON.createObjectNode()
                .put("id", MEMBERSHIP_SCOPE_ID)
                .put("name", "membership:write")
                .put("protocol", "openid-connect");
        storedClientScope.putObject("attributes")
                .put("identityhub.managed", "false");

        assertThatThrownBy(() -> projector().project(machineSnapshot()))
                .isInstanceOf(ApplicationClientProjectionException.class)
                .satisfies(exception -> assertThat(
                                ((ApplicationClientProjectionException) exception).retryable())
                        .isFalse());
        assertThat(clientScopeCreates).hasValue(0);
        assertThat(defaultScopeAttachments).hasValue(0);
        assertThat(storedClient).isNull();
    }

    @Test
    void rotatesBffSecretAndReturnsItOnlyToTheCaller() {
        var projector = projector();
        projector.project(bffSnapshot());

        var secret = projector.rotate(bffSnapshot());

        assertThat(secret.value()).isEqualTo("one-time-generated-secret");
        assertThat(secret.toString()).isEqualTo("[REDACTED]");
        assertThat(rotations).hasValue(1);
        assertThat(storedClient.path("secret").isMissingNode()).isTrue();
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
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                true,
                Instant.parse("2026-07-31T16:00:00Z"),
                UUID.fromString("27f3aa0b-6a70-43bd-a087-d5bc0c1bc779"),
                1,
                "keycloak-adapter-test",
                ApplicationClientProjectionState.PENDING,
                0,
                Instant.parse("2026-07-31T16:00:00Z"),
                null);
    }

    private ApplicationClientSnapshot spaSnapshot() {
        return new ApplicationClientSnapshot(
                UUID.fromString("ff7c4748-f053-4fb6-91be-d34cf0015834"),
                UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0"),
                "catalog-web",
                "SPA",
                null,
                java.util.List.of("https://app.example.com/auth/callback"),
                java.util.List.of("https://app.example.com"),
                java.util.List.of(),
                true,
                Instant.parse("2026-07-31T16:00:00Z"),
                UUID.fromString("27f3aa0b-6a70-43bd-a087-d5bc0c1bc779"),
                1,
                "keycloak-spa-test",
                ApplicationClientProjectionState.PENDING,
                0,
                Instant.parse("2026-07-31T16:00:00Z"),
                null);
    }

    private ApplicationClientSnapshot bffSnapshot() {
        return new ApplicationClientSnapshot(
                UUID.fromString("72c43df3-9f34-4dc6-85cc-5d323762f299"),
                UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0"),
                "catalog-bff",
                "BFF",
                null,
                java.util.List.of(
                        "https://app.example.com/login/oauth2/code/identityhub"),
                java.util.List.of(),
                java.util.List.of(),
                true,
                Instant.parse("2026-08-01T14:00:00Z"),
                UUID.fromString("92390c62-b1f7-48d4-887a-d004a47faf8b"),
                1,
                "keycloak-bff-test",
                ApplicationClientProjectionState.PENDING,
                0,
                Instant.parse("2026-08-01T14:00:00Z"),
                null);
    }

    private ApplicationClientSnapshot machineSnapshot() {
        return new ApplicationClientSnapshot(
                UUID.fromString("72c43df3-9f34-4dc6-85cc-5d323762f299"),
                UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0"),
                "catalog-membership-provisioner",
                "MACHINE",
                null,
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of("membership:write"),
                true,
                Instant.parse("2026-08-01T14:00:00Z"),
                UUID.fromString("92390c62-b1f7-48d4-887a-d004a47faf8b"),
                1,
                "keycloak-machine-test",
                ApplicationClientProjectionState.PENDING,
                0,
                Instant.parse("2026-08-01T14:00:00Z"),
                null);
    }

    private void token(HttpExchange exchange) throws IOException {
        respond(exchange, 200, "{\"access_token\":\"synthetic-management-token\"}");
    }

    private void clients(HttpExchange exchange) throws IOException {
        if (managementStatus != 200) {
            respond(exchange, managementStatus, "sensitive upstream details");
            return;
        }
        if (exchange.getRequestURI().getPath().endsWith("/client-secret")
                && "POST".equals(exchange.getRequestMethod())) {
            rotations.incrementAndGet();
            respond(exchange, 200, "{\"value\":\"one-time-generated-secret\"}");
            return;
        }
        if (exchange.getRequestURI().getPath().contains("/default-client-scopes")) {
            defaultClientScopes(exchange);
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
            respond(exchange, failAfterCreate ? 503 : 201, "");
            return;
        }
        if ("PUT".equals(exchange.getRequestMethod())) {
            updates.incrementAndGet();
            respond(exchange, 204, "");
            return;
        }
        respond(exchange, 405, "");
    }

    private void clientScopes(HttpExchange exchange) throws IOException {
        var collection = exchange.getRequestURI().getPath().endsWith("/client-scopes");
        if ("GET".equals(exchange.getRequestMethod())) {
            var response = storedClientScope == null
                    ? (collection ? "[]" : "{}")
                    : (collection ? "[" + storedClientScope + "]" : storedClientScope.toString());
            respond(exchange, 200, response);
            return;
        }
        var received = (ObjectNode) JSON.readTree(exchange.getRequestBody());
        received.put("id", MEMBERSHIP_SCOPE_ID);
        storedClientScope = received;
        if ("POST".equals(exchange.getRequestMethod())) {
            clientScopeCreates.incrementAndGet();
            respond(exchange, 201, "");
            return;
        }
        if ("PUT".equals(exchange.getRequestMethod())) {
            respond(exchange, 204, "");
            return;
        }
        respond(exchange, 405, "");
    }

    private void defaultClientScopes(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            var scopes = new java.util.ArrayList<String>();
            if (profileScopeAttached) {
                scopes.add("{\"id\":\"profile-scope-uuid\",\"name\":\"profile\"}");
            }
            if (membershipScopeAttached) {
                scopes.add("{\"id\":\"" + MEMBERSHIP_SCOPE_ID
                        + "\",\"name\":\"membership:write\"}");
            }
            var response = "[" + String.join(",", scopes) + "]";
            respond(exchange, 200, response);
            return;
        }
        if ("PUT".equals(exchange.getRequestMethod())) {
            membershipScopeAttached = true;
            defaultScopeAttachments.incrementAndGet();
            respond(exchange, 204, "");
            return;
        }
        if ("DELETE".equals(exchange.getRequestMethod())) {
            if (exchange.getRequestURI().getPath().endsWith("profile-scope-uuid")) {
                profileScopeAttached = false;
            } else {
                membershipScopeAttached = false;
            }
            defaultScopeRemovals.incrementAndGet();
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
