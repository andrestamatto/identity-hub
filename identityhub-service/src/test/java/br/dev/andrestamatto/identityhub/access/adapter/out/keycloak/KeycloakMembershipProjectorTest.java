package br.dev.andrestamatto.identityhub.access.adapter.out.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.dev.andrestamatto.identityhub.access.application.MembershipProjectionException;
import br.dev.andrestamatto.identityhub.access.application.MembershipProjectionFailureCode;
import br.dev.andrestamatto.identityhub.access.domain.Membership;
import br.dev.andrestamatto.identityhub.access.domain.MembershipApplicationRef;
import br.dev.andrestamatto.identityhub.access.domain.MembershipId;
import br.dev.andrestamatto.identityhub.access.domain.MembershipUserAccountRef;
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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

class KeycloakMembershipProjectorTest {

    private static final String REALM = "identityhub-test";
    private static final ObjectMapper JSON = new ObjectMapper();
    private final AtomicInteger creates = new AtomicInteger();
    private final AtomicInteger joins = new AtomicInteger();
    private final AtomicInteger roleMappings = new AtomicInteger();
    private HttpServer server;
    private ObjectNode group;
    private boolean userExists = true;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/realms/" + REALM + "/protocol/openid-connect/token",
                exchange -> respond(exchange, 200, "{\"access_token\":\"token\"}"));
        server.createContext("/admin/realms/" + REALM + "/groups", this::groups);
        server.createContext("/admin/realms/" + REALM + "/users/", this::users);
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void createsOwnedMarkerAndJoinsUserIdempotently() {
        var projector = projector();

        projector.project(membership());
        projector.project(membership());

        assertThat(creates).hasValue(1);
        assertThat(joins).hasValue(2);
        assertThat(group.path("attributes").path("identityhub.application-id").get(0)
                .asString()).isEqualTo(membership().applicationRef().value().toString());
    }

    @Test
    void mapsPreparedClientRoleThroughIdentityBoundary() {
        var role = new KeycloakClientRole(
                "api-internal", "role-id", "ih-membership-access");

        projector().project(membership(), java.util.List.of(role));

        assertThat(joins).hasValue(1);
        assertThat(roleMappings).hasValue(1);
    }

    @Test
    void rejectsUnknownUserWithoutCreatingMarker() {
        userExists = false;

        assertThatThrownBy(() -> projector().project(membership()))
                .isInstanceOfSatisfying(MembershipProjectionException.class,
                        exception -> assertThat(exception.failureCode())
                                .isEqualTo(MembershipProjectionFailureCode.USER_NOT_FOUND));
        assertThat(creates).hasValue(0);
    }

    @Test
    void refusesHomonymousUnownedMarker() {
        group = JSON.createObjectNode()
                .put("id", "group-uuid")
                .put("name", markerName());

        assertThatThrownBy(() -> projector().project(membership()))
                .isInstanceOfSatisfying(MembershipProjectionException.class,
                        exception -> assertThat(exception.failureCode())
                                .isEqualTo(MembershipProjectionFailureCode.MARKER_CONFLICT));
        assertThat(joins).hasValue(0);
    }

    private KeycloakMembershipProjector projector() {
        return new KeycloakMembershipProjector(
                HttpClient.newHttpClient(), JSON, baseUri(), REALM, "identity-manager", "secret");
    }

    private Membership membership() {
        return Membership.request(
                new MembershipId(UUID.fromString("c50638fe-0b91-4f47-81e6-2bd183040c1c")),
                new MembershipApplicationRef(
                        UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0")),
                new MembershipUserAccountRef(
                        UUID.fromString("680ac2e4-bfb0-4375-a75e-453b6e7b600c")),
                Clock.fixed(Instant.parse("2026-08-03T01:00:00Z"), ZoneOffset.UTC));
    }

    private void groups(HttpExchange exchange) throws IOException {
        if (exchange.getRequestURI().getPath().contains("/role-mappings/clients/")) {
            roleMappings.incrementAndGet();
            respond(exchange, 204, "");
            return;
        }
        if ("GET".equals(exchange.getRequestMethod())) {
            respond(exchange, 200, group == null ? "[]" : "[" + group + "]");
            return;
        }
        group = (ObjectNode) JSON.readTree(exchange.getRequestBody());
        group.put("id", "group-uuid");
        creates.incrementAndGet();
        respond(exchange, 201, "");
    }

    private void users(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            respond(exchange, userExists ? 200 : 404, userExists ? "{\"id\":\"user\"}" : "");
            return;
        }
        joins.incrementAndGet();
        respond(exchange, 204, "");
    }

    private String markerName() {
        return "ih-membership-" + membership().applicationRef().value();
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
