package br.dev.andrestamatto.identityhub.access.adapter.out.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.dev.andrestamatto.identityhub.access.application.MembershipProjectionException;
import br.dev.andrestamatto.identityhub.access.application.MembershipProjectionFailureCode;
import br.dev.andrestamatto.identityhub.access.domain.Membership;
import br.dev.andrestamatto.identityhub.access.domain.MembershipApplicationRef;
import br.dev.andrestamatto.identityhub.access.domain.MembershipId;
import br.dev.andrestamatto.identityhub.access.domain.MembershipUserAccountRef;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationTokenClient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class KeycloakMembershipTokenProjectorTest {

    private static final String REALM = "identityhub-test";
    private static final UUID API_ID =
            UUID.fromString("20e6f465-84be-4c97-b8fd-37b618d2bd91");
    private static final UUID SPA_ID =
            UUID.fromString("81bd51f0-280d-48bd-9e16-4763667a1c10");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final AtomicInteger roleCreates = new AtomicInteger();
    private final AtomicInteger scopeCreates = new AtomicInteger();
    private final AtomicInteger scopeUpdates = new AtomicInteger();
    private final AtomicInteger scopeRoleMappings = new AtomicInteger();
    private final AtomicInteger scopeAttachments = new AtomicInteger();
    private HttpServer server;
    private boolean roleCreated;
    private boolean scopeCreated;
    private boolean scopeAttached;
    private boolean clientOwned = true;
    private boolean unexpectedScopeMapper;
    private boolean scopeReconciled;
    private JsonNode lastScopeUpdate;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void projectsOwnedRoleScopedAudienceIdempotently() {
        var projector = projector();

        projector.project(membership());
        projector.project(membership());

        assertThat(roleCreates).hasValue(1);
        assertThat(scopeCreates).hasValue(1);
        assertThat(scopeRoleMappings).hasValue(2);
        assertThat(scopeAttachments).hasValue(1);
    }

    @Test
    void refusesToConfigureUnownedApplicationClient() {
        clientOwned = false;

        assertThatThrownBy(() -> projector().project(membership()))
                .isInstanceOfSatisfying(MembershipProjectionException.class,
                        exception -> assertThat(exception.failureCode()).isEqualTo(
                                MembershipProjectionFailureCode
                                        .TOKEN_CONFIGURATION_CONFLICT));
        assertThat(roleCreates).hasValue(0);
        assertThat(scopeCreates).hasValue(0);
    }

    @Test
    void reconcilesAnUnexpectedScopeMapper() {
        scopeCreated = true;
        unexpectedScopeMapper = true;

        projector().project(membership());

        assertThat(scopeUpdates).hasValue(1);
        assertThat(lastScopeUpdate.path("protocolMappers"))
                .extracting(mapper -> mapper.path("protocolMapper").asString())
                .containsExactlyInAnyOrder(
                        "oidc-audience-resolve-mapper",
                        "oidc-hardcoded-claim-mapper");
    }

    private KeycloakMembershipTokenProjector projector() {
        return new KeycloakMembershipTokenProjector(
                HttpClient.newHttpClient(),
                JSON,
                baseUri(),
                REALM,
                "identity-manager",
                "secret",
                applicationId -> List.of(
                        new ApplicationTokenClient(API_ID, "API", "catalog-api"),
                        new ApplicationTokenClient(SPA_ID, "SPA", null)));
    }

    private void handle(HttpExchange exchange) throws IOException {
        var path = exchange.getRequestURI().getPath();
        var method = exchange.getRequestMethod();
        if (path.endsWith("/protocol/openid-connect/token")) {
            respond(exchange, 200, "{\"access_token\":\"token\"}");
        } else if (path.endsWith("/clients") && "GET".equals(method)) {
            respond(exchange, 200, pathClient(exchange.getRequestURI().getQuery()));
        } else if (path.endsWith("/roles/ih-membership-access") && "GET".equals(method)) {
            respond(exchange, roleCreated ? 200 : 404, roleCreated ? managedRole() : "");
        } else if (path.endsWith("/roles") && "POST".equals(method)) {
            roleCreated = true;
            roleCreates.incrementAndGet();
            respond(exchange, 201, "");
        } else if (path.endsWith("/client-scopes") && "GET".equals(method)) {
            respond(exchange, 200, scopeCreated ? "[" + managedScopeSummary() + "]" : "[]");
        } else if (path.endsWith("/client-scopes") && "POST".equals(method)) {
            scopeCreated = true;
            scopeCreates.incrementAndGet();
            respond(exchange, 201, "");
        } else if (path.endsWith("/client-scopes/scope-id") && "GET".equals(method)) {
            respond(exchange, 200, managedScopeDetail());
        } else if (path.endsWith("/client-scopes/scope-id") && "PUT".equals(method)) {
            scopeReconciled = true;
            lastScopeUpdate = JSON.readTree(exchange.getRequestBody());
            scopeUpdates.incrementAndGet();
            respond(exchange, 204, "");
        } else if (path.contains("/scope-mappings/clients/")) {
            scopeRoleMappings.incrementAndGet();
            respond(exchange, 204, "");
        } else if (path.endsWith("/default-client-scopes") && "GET".equals(method)) {
            respond(exchange, 200, scopeAttached
                    ? "[{\"id\":\"basic-id\",\"name\":\"basic\"},"
                            + managedScopeSummary() + "]"
                    : "[{\"id\":\"basic-id\",\"name\":\"basic\"},"
                            + "{\"id\":\"realm-default\",\"name\":\"profile\"}]");
        } else if (path.endsWith("/optional-client-scopes") && "GET".equals(method)) {
            respond(exchange, 200, "[{\"id\":\"realm-optional\"}]");
        } else if (path.contains("/default-client-scopes/scope-id")
                && "PUT".equals(method)) {
            scopeAttached = true;
            scopeAttachments.incrementAndGet();
            respond(exchange, 204, "");
        } else if ("DELETE".equals(method)) {
            respond(exchange, 204, "");
        } else {
            respond(exchange, 404, "");
        }
    }

    private String pathClient(String query) {
        if (query.contains("catalog-api")) {
            return "[" + managedClient("api-internal", API_ID, "API", "catalog-api") + "]";
        }
        return "[" + managedClient(
                "spa-internal", SPA_ID, "SPA", "ih-spa-" + SPA_ID) + "]";
    }

    private String managedClient(
            String internalId,
            UUID clientId,
            String type,
            String projectedClientId) {
        return """
                {"id":"%s","clientId":"%s","fullScopeAllowed":false,
                 "attributes":{"identityhub.managed":"%s",
                 "identityhub.application-client-id":"%s",
                 "identityhub.application-client-type":"%s"}}
                """.formatted(
                internalId,
                projectedClientId,
                clientOwned ? "true" : "false",
                clientId,
                type);
    }

    private String managedRole() {
        return """
                {"id":"role-id","name":"ih-membership-access",
                 "attributes":{"identityhub.managed":["true"],
                 "identityhub.application-id":["%s"]}}
                """.formatted(applicationId());
    }

    private String managedScopeSummary() {
        return """
                {"id":"scope-id","name":"ih-access-%s"}
                """.formatted(applicationId());
    }

    private String managedScopeDetail() {
        return """
                {"id":"scope-id","name":"ih-access-%s","protocol":"openid-connect",
                 "attributes":{"identityhub.managed":"true",
                 "identityhub.application-id":"%s","include.in.token.scope":"false"},
                 "protocolMappers":[
                    {"protocolMapper":"oidc-audience-resolve-mapper",
                     "config":{"access.token.claim":"true","id.token.claim":"false",
                     "introspection.token.claim":"true"}},
                    {"protocolMapper":"oidc-hardcoded-claim-mapper",
                     "config":{"claim.name":"roles","claim.value":"[]",
                     "jsonType.label":"JSON","access.token.claim":"true",
                     "id.token.claim":"false","userinfo.token.claim":"false",
                     "introspection.token.claim":"true"}}%s
                  ]}
                """.formatted(
                applicationId(),
                applicationId(),
                unexpectedScopeMapper && !scopeReconciled
                        ? ",{\"protocolMapper\":\"oidc-usermodel-attribute-mapper\"}"
                        : "");
    }

    private Membership membership() {
        return Membership.request(
                new MembershipId(UUID.fromString("c50638fe-0b91-4f47-81e6-2bd183040c1c")),
                new MembershipApplicationRef(applicationId()),
                new MembershipUserAccountRef(
                        UUID.fromString("680ac2e4-bfb0-4375-a75e-453b6e7b600c")),
                Clock.fixed(Instant.parse("2026-08-03T01:00:00Z"), ZoneOffset.UTC));
    }

    private UUID applicationId() {
        return UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0");
    }

    private URI baseUri() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
