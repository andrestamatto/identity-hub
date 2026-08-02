package br.dev.andrestamatto.identityhub.identity.adapter.out.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.dev.andrestamatto.identityhub.identity.application.LocalIdentityRegistrationException;
import br.dev.andrestamatto.identityhub.identity.application.LocalIdentityRegistrationFailureCode;
import br.dev.andrestamatto.identityhub.identity.application.LocalIdentityVerificationException;
import br.dev.andrestamatto.identityhub.identity.application.LocalPasswordResetException;
import br.dev.andrestamatto.identityhub.identity.application.PendingLocalIdentity;
import br.dev.andrestamatto.identityhub.identity.domain.LocalPassword;
import br.dev.andrestamatto.identityhub.identity.domain.LoginEmail;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class KeycloakLocalIdentityGatewayTest {

    private static final String REALM = "identityhub-test";
    private static final UUID USER_ID =
            UUID.fromString("9bc4a8c9-405b-4f4a-b443-3c2012369264");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final AtomicInteger creates = new AtomicInteger();
    private final AtomicInteger updates = new AtomicInteger();
    private final List<String> credentialMutations = new ArrayList<>();
    private HttpServer server;
    private JsonNode storedUser;
    private int managementStatus = 200;
    private int createStatus = 201;
    private int logoutStatus = 204;
    private boolean passwordCredentialCreated;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/realms/" + REALM + "/protocol/openid-connect/token",
                this::token);
        server.createContext(
                "/admin/realms/" + REALM + "/users",
                this::users);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void createsDisabledUnverifiedUserAndReplaysWithoutChangingCredential() {
        var registrar = registrar();

        var first = register(registrar);
        var replay = register(registrar);

        assertThat(first.created()).isTrue();
        assertThat(replay.created()).isFalse();
        assertThat(replay.userAccountRef()).isEqualTo(first.userAccountRef());
        assertThat(creates).hasValue(1);
        assertThat(storedUser.path("username").asString()).isEqualTo("andre@example.com");
        assertThat(storedUser.path("email").asString()).isEqualTo("Andre@Example.com");
        assertThat(storedUser.path("enabled").asBoolean()).isFalse();
        assertThat(storedUser.path("emailVerified").asBoolean()).isFalse();
        assertThat(storedUser.path("credentials").get(0).path("temporary").asBoolean())
                .isFalse();
    }

    @Test
    void classifiesProviderFailureWithoutLeakingResponse() {
        managementStatus = 503;

        assertThatThrownBy(() -> register(registrar()))
                .isInstanceOfSatisfying(
                        LocalIdentityRegistrationException.class,
                        exception -> {
                            assertThat(exception.retryable()).isTrue();
                            assertThat(exception.failureCode())
                                    .isEqualTo(LocalIdentityRegistrationFailureCode
                                            .PROVIDER_UNAVAILABLE);
                            assertThat(exception.getMessage()).doesNotContain("provider-body");
                        });
    }

    @Test
    void classifiesUnresolvedCreateConflictAsPermanent() {
        createStatus = 409;

        assertThatThrownBy(() -> register(registrar()))
                .isInstanceOfSatisfying(
                        LocalIdentityRegistrationException.class,
                        exception -> {
                            assertThat(exception.retryable()).isFalse();
                            assertThat(exception.failureCode())
                                    .isEqualTo(LocalIdentityRegistrationFailureCode
                                            .IDENTITY_CONFLICT);
                        });
    }

    @Test
    void verifiesAndEnablesUserIdempotently() {
        var registrar = registrar();
        var registration = register(registrar);

        registrar.verifyAndEnable(
                registration.userAccountRef(), new LoginEmail("andre@example.com"));
        registrar.verifyAndEnable(
                registration.userAccountRef(), new LoginEmail("andre@example.com"));

        assertThat(storedUser.path("enabled").asBoolean()).isTrue();
        assertThat(storedUser.path("emailVerified").asBoolean()).isTrue();
        assertThat(updates).hasValue(1);
    }

    @Test
    void refusesToVerifyWhenCurrentEmailDoesNotMatchChallenge() {
        var registrar = registrar();
        var registration = register(registrar);

        assertThatThrownBy(() -> registrar.verifyAndEnable(
                        registration.userAccountRef(),
                        new LoginEmail("another@example.com")))
                .isInstanceOfSatisfying(
                        LocalIdentityVerificationException.class,
                        exception -> assertThat(exception.retryable()).isFalse());
        assertThat(updates).hasValue(0);
        assertThat(storedUser.path("enabled").asBoolean()).isFalse();
    }

    @Test
    void findsOnlyEnabledVerifiedLocalIdentityForPasswordRecovery() {
        var registrar = registrar();
        var registration = register(registrar);

        assertThat(registrar.findEligible(new LoginEmail("andre@example.com"))).isEmpty();

        registrar.verifyAndEnable(
                registration.userAccountRef(), new LoginEmail("andre@example.com"));

        assertThat(registrar.findEligible(new LoginEmail("Andre@Example.com")))
                .get()
                .satisfies(identity -> {
                    assertThat(identity.userAccountRef()).isEqualTo(
                            registration.userAccountRef());
                    assertThat(identity.email().normalizedValue())
                            .isEqualTo("andre@example.com");
                });
    }

    @Test
    void revokesSessionsBeforeResettingPassword() {
        var registrar = registrar();
        var registration = register(registrar);
        registrar.verifyAndEnable(
                registration.userAccountRef(), new LoginEmail("andre@example.com"));

        try (var password = new LocalPassword(
                "uma nova frase longa e segura".toCharArray())) {
            registrar.reset(
                    registration.userAccountRef(),
                    new LoginEmail("andre@example.com"),
                    password);
        }

        assertThat(credentialMutations).containsExactly("logout", "reset-password");
    }

    @Test
    void refusesResetWhenVerifiedIdentityNoLongerMatches() {
        var registrar = registrar();
        var registration = register(registrar);
        registrar.verifyAndEnable(
                registration.userAccountRef(), new LoginEmail("andre@example.com"));

        try (var password = new LocalPassword(
                "uma nova frase longa e segura".toCharArray())) {
            assertThatThrownBy(() -> registrar.reset(
                            registration.userAccountRef(),
                            new LoginEmail("other@example.com"),
                            password))
                    .isInstanceOf(LocalPasswordResetException.class);
        }
        assertThat(credentialMutations).isEmpty();
    }

    @Test
    void doesNotResetPasswordWhenSessionRevocationFails() {
        var registrar = registrar();
        var registration = register(registrar);
        registrar.verifyAndEnable(
                registration.userAccountRef(), new LoginEmail("andre@example.com"));
        logoutStatus = 503;

        try (var password = new LocalPassword(
                "uma nova frase longa e segura".toCharArray())) {
            assertThatThrownBy(() -> registrar.reset(
                            registration.userAccountRef(),
                            new LoginEmail("andre@example.com"),
                            password))
                    .isInstanceOf(LocalPasswordResetException.class);
        }
        assertThat(credentialMutations).containsExactly("logout");
    }

    private KeycloakLocalIdentityGateway registrar() {
        return new KeycloakLocalIdentityGateway(
                HttpClient.newHttpClient(),
                JSON,
                URI.create("http://127.0.0.1:" + server.getAddress().getPort()),
                REALM,
                "identity-management",
                "management-secret");
    }

    private br.dev.andrestamatto.identityhub.identity.application.LocalIdentityRegistration
            register(KeycloakLocalIdentityGateway registrar) {
        try (var password = new LocalPassword(
                "frase longa com café seguro".toCharArray())) {
            return registrar.register(new PendingLocalIdentity(
                    new LoginEmail("Andre@Example.com"), password));
        }
    }

    private void token(HttpExchange exchange) throws IOException {
        send(exchange, 200, "{\"access_token\":\"management-token\"}");
    }

    private void users(HttpExchange exchange) throws IOException {
        if (managementStatus != 200) {
            send(exchange, managementStatus, "provider-body-must-not-leak");
            return;
        }
        if (exchange.getRequestURI().getPath().endsWith(USER_ID + "/credentials")) {
            send(exchange, 200, passwordCredentialCreated
                    ? "[{\"type\":\"password\"}]" : "[]");
            return;
        }
        if (exchange.getRequestURI().getPath().endsWith(USER_ID + "/logout")) {
            credentialMutations.add("logout");
            send(exchange, logoutStatus, "");
            return;
        }
        if (exchange.getRequestURI().getPath().endsWith(USER_ID + "/reset-password")) {
            credentialMutations.add("reset-password");
            var credential = JSON.readTree(exchange.getRequestBody().readAllBytes());
            assertThat(credential.path("type").asString()).isEqualTo("password");
            assertThat(credential.path("temporary").asBoolean()).isFalse();
            assertThat(credential.path("value").asString())
                    .isEqualTo("uma nova frase longa e segura");
            send(exchange, 204, "");
            return;
        }
        if (exchange.getRequestURI().getPath().endsWith(USER_ID.toString())) {
            if (exchange.getRequestMethod().equals("GET")) {
                send(exchange, 200, JSON.writeValueAsString(java.util.Map.of(
                        "id", USER_ID.toString(),
                        "username", storedUser.path("username").asString(),
                        "email", storedUser.path("email").asString(),
                        "enabled", storedUser.path("enabled").asBoolean(),
                        "emailVerified", storedUser.path("emailVerified").asBoolean(),
                        "attributes", storedUser.path("attributes"))));
                return;
            }
            storedUser = JSON.readTree(exchange.getRequestBody().readAllBytes());
            updates.incrementAndGet();
            send(exchange, 204, "");
            return;
        }
        if (exchange.getRequestMethod().equals("GET")) {
            var response = storedUser == null
                    ? "[]"
                    : "[" + JSON.writeValueAsString(storedUser) + "]";
            send(exchange, 200, response);
            return;
        }
        if (createStatus != 201) {
            send(exchange, createStatus, "create-conflict-body-must-not-leak");
            return;
        }
        storedUser = JSON.readTree(exchange.getRequestBody().readAllBytes());
        passwordCredentialCreated = true;
        ((tools.jackson.databind.node.ObjectNode) storedUser)
                .put("id", USER_ID.toString());
        creates.incrementAndGet();
        exchange.getResponseHeaders().add(
                "Location",
                "/admin/realms/" + REALM + "/users/" + USER_ID);
        send(exchange, 201, "");
    }

    private void send(HttpExchange exchange, int status, String body) throws IOException {
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
