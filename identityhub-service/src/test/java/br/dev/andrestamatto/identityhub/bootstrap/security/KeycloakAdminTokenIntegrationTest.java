package br.dev.andrestamatto.identityhub.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.dev.andrestamatto.identityhub.clientapplication.adapter.out.keycloak.KeycloakApplicationClientProjector;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientProjectionState;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientSnapshot;
import br.dev.andrestamatto.identityhub.identity.adapter.out.keycloak.KeycloakLocalIdentityRegistrar;
import br.dev.andrestamatto.identityhub.identity.application.LocalIdentityRegistration;
import br.dev.andrestamatto.identityhub.identity.application.PendingLocalIdentity;
import br.dev.andrestamatto.identityhub.identity.domain.LocalPassword;
import br.dev.andrestamatto.identityhub.identity.domain.LoginEmail;
import br.dev.andrestamatto.identityhub.identity.domain.UserAccountRef;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KeycloakAdminTokenIntegrationTest {

    private static final String REALM = "identityhub-test";
    private static final String CLIENT_ID = "identityhub-admin-login";
    private static final String ADMIN_AUDIENCE = "identityhub-admin-api";
    private static final String MANAGEMENT_CLIENT_ID = "identityhub-management";
    private static final String MANAGEMENT_CLIENT_SECRET = syntheticPassword();
    private static final String IDENTITY_MANAGEMENT_CLIENT_ID =
            "identityhub-identity-management";
    private static final String IDENTITY_MANAGEMENT_CLIENT_SECRET = syntheticPassword();
    private static final String USERNAME = "platform-admin";
    private static final String PASSWORD = syntheticPassword();
    private static final String OTP_SECRET = String.join("", "JBSWY3DP", "EHPK3PXP");
    private static final String REDIRECT_URI = "http://127.0.0.1/callback";
    private static final String PKCE_VERIFIER =
            "test-only-verifier-identityhub-admin-login-0001";
    private static final String LOCAL_LOGIN_USERNAME = "verified.user@example.test";
    private static final String LOCAL_LOGIN_PASSWORD = "test-only-verified-user-password";
    private static final String BRUTE_FORCE_USERNAME = "brute.force.user@example.test";
    private static final String BRUTE_FORCE_PASSWORD = "test-only-brute-force-user-password";
    private static final String LOCAL_LOGIN_REDIRECT_URI =
            "http://127.0.0.1:5173/auth/callback";
    private static final String LOCAL_LOGIN_PKCE_VERIFIER =
            "test-only-verifier-identityhub-local-login-0001";
    private static final UUID LOCAL_LOGIN_CLIENT_ID =
            UUID.fromString("7167c098-8e18-4356-8fa2-2b3425273257");
    private static final String BOOTSTRAP_ADMIN = "test-bootstrap-admin";
    private static final String BOOTSTRAP_PASSWORD = syntheticPassword();
    private static final Network NETWORK = Network.newNetwork();
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern FORM_ACTION =
            Pattern.compile("<form[^>]+action=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);

    @Container
    private static final PostgreSQLContainer KEYCLOAK_DATABASE =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17.10"))
                    .withNetwork(NETWORK)
                    .withNetworkAliases("keycloak-database");

    @Container
    private static final PostgreSQLContainer CONTROL_DATABASE =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17.10"));

    @Container
    private static final GenericContainer<?> KEYCLOAK =
            new GenericContainer<>(
                    DockerImageName.parse("quay.io/keycloak/keycloak:26.7.0"))
                    .dependsOn(KEYCLOAK_DATABASE)
                    .withNetwork(NETWORK)
                    .withEnv("KC_DB", "postgres")
                    .withEnv(
                            "KC_DB_URL",
                            "jdbc:postgresql://keycloak-database:5432/"
                                    + KEYCLOAK_DATABASE.getDatabaseName())
                    .withEnv("KC_DB_USERNAME", KEYCLOAK_DATABASE.getUsername())
                    .withEnv("KC_DB_PASSWORD", KEYCLOAK_DATABASE.getPassword())
                    .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", BOOTSTRAP_ADMIN)
                    .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", BOOTSTRAP_PASSWORD)
                    .withCommand("start", "--http-enabled=true", "--hostname-strict=false")
                    .withExposedPorts(8080)
                    .waitingFor(Wait.forHttp("/realms/master/.well-known/openid-configuration")
                            .forStatusCode(200)
                            .withStartupTimeout(Duration.ofMinutes(4)));

    private static URI keycloakBaseUri;

    @LocalServerPort
    private int servicePort;

    @Autowired
    private JdbcClient jdbcClient;

    @DynamicPropertySource
    static void configureApplication(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", CONTROL_DATABASE::getJdbcUrl);
        registry.add("spring.datasource.username", CONTROL_DATABASE::getUsername);
        registry.add("spring.datasource.password", CONTROL_DATABASE::getPassword);
        registry.add(
                "identityhub.security.admin.issuer-uri",
                () -> realmIssuer().toString());
        registry.add(
                "identityhub.security.admin.jwk-set-uri",
                () -> realmIssuer() + "/protocol/openid-connect/certs");
        registry.add(
                "identityhub.security.admin.audience",
                () -> ADMIN_AUDIENCE);
        registry.add("identityhub.keycloak.management.enabled", () -> "true");
        registry.add(
                "identityhub.keycloak.management.base-uri",
                () -> keycloakBaseUri().toString());
        registry.add("identityhub.keycloak.management.realm", () -> REALM);
        registry.add(
                "identityhub.keycloak.management.client-id",
                () -> MANAGEMENT_CLIENT_ID);
        registry.add(
                "identityhub.keycloak.management.client-secret",
                () -> MANAGEMENT_CLIENT_SECRET);
        registry.add(
                "identityhub.keycloak.identity-management.public-base-uri",
                () -> "http://127.0.0.1");
        registry.add("identityhub.keycloak.management.poll-interval", () -> "PT1S");
        registry.add("identityhub.keycloak.management.lease-duration", () -> "PT30S");
        registry.add("identityhub.keycloak.management.initial-retry-delay", () -> "PT1S");
        registry.add("identityhub.keycloak.management.max-attempts", () -> "5");
    }

    @BeforeAll
    static void configureRealm() throws Exception {
        keycloakBaseUri = keycloakBaseUri();
        var adminToken = requestBootstrapAdminToken();
        createRealm(adminToken);
        configureEmailOnlyUserProfile(adminToken);
        configureGenericLoginMessages(adminToken);
        grantClientManagement(adminToken);
        grantIdentityManagement(adminToken);
        configureAuthenticationMethodReferences(adminToken);
    }

    @Test
    void acceptsRealKeycloakTokenWithExpectedAudienceRoleAndTotp() throws Exception {
        var accessToken = authenticateThroughHostedLogin();
        var issuer = realmIssuer();
        var properties = new AdminSecurityProperties(
                issuer,
                URI.create(issuer + "/protocol/openid-connect/certs"),
                ADMIN_AUDIENCE);

        Jwt jwt = new AdminSecurityConfiguration()
                .adminJwtDecoder(properties)
                .decode(accessToken);
        var authentication = new AdminJwtAuthenticationConverter().convert(jwt);

        assertThat(jwt.getClaimAsString("iss")).isEqualTo(issuer.toString());
        assertThat(jwt.getClaimAsStringList("aud")).containsExactly(ADMIN_AUDIENCE);
        assertThat(jwt.getClaimAsStringList("amr")).contains("pwd", "totp");
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_PLATFORM_ADMIN", "MFA_TOTP");
        assertTokenIsolation(accessToken, issuer);

        var correlationId = "real-keycloak-admin-request";
        var response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(
                                URI.create("http://127.0.0.1:" + servicePort
                                        + "/internal/admin/runtime"))
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Correlation-ID", correlationId)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"environment\":\"DEVELOPMENT\"");
        assertThat(jdbcClient.sql("""
                            select outcome
                            from administrative_access_event
                            where correlation_id = :correlationId
                            """)
                .param("correlationId", correlationId)
                .query(String.class)
                .single())
                .isEqualTo("ALLOWED");

        assertClientApplicationAdministration(accessToken);

        var deniedCorrelationId = "anonymous-admin-request";
        var deniedResponse = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(
                                URI.create("http://127.0.0.1:" + servicePort
                                        + "/internal/admin/runtime"))
                        .header("X-Correlation-ID", deniedCorrelationId)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.discarding());

        assertThat(deniedResponse.statusCode()).isEqualTo(401);
        assertThat(jdbcClient.sql("""
                            select outcome
                            from administrative_access_event
                            where correlation_id = :correlationId
                            """)
                .param("correlationId", deniedCorrelationId)
                .query(String.class)
                .single())
                .isEqualTo("DENIED");
    }

    @Test
    void projectsProtectedApiUsingLeastPrivilegeServiceAccount() throws Exception {
        var clientId = UUID.fromString("ff7c4748-f053-4fb6-91be-d34cf0015834");
        var snapshot = new ApplicationClientSnapshot(
                clientId,
                UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0"),
                "catalog-api",
                "API",
                "catalog-api",
                List.of(),
                List.of(),
                true,
                Instant.parse("2026-07-31T16:00:00Z"),
                UUID.fromString("27f3aa0b-6a70-43bd-a087-d5bc0c1bc779"),
                1,
                "keycloak-integration-test",
                ApplicationClientProjectionState.PENDING,
                0,
                Instant.parse("2026-07-31T16:00:00Z"),
                null);
        var projector = new KeycloakApplicationClientProjector(
                HttpClient.newHttpClient(),
                JSON,
                keycloakBaseUri,
                REALM,
                MANAGEMENT_CLIENT_ID,
                MANAGEMENT_CLIENT_SECRET);

        projector.project(snapshot);
        projector.project(snapshot);

        var adminToken = requestBootstrapAdminToken();
        var response = httpClient(HttpClient.Redirect.NORMAL).send(
                authorizedGetRequest(
                        keycloakBaseUri.resolve(
                                "/admin/realms/" + REALM + "/clients?clientId=ih-api-"
                                        + clientId + "&exact=true"),
                        adminToken),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        var projected = JSON.readTree(response.body()).required(0);
        assertThat(projected.path("bearerOnly").asBoolean()).isTrue();
        assertThat(projected.path("standardFlowEnabled").asBoolean()).isFalse();
        assertThat(projected.path("directAccessGrantsEnabled").asBoolean()).isFalse();
        assertThat(projected.path("serviceAccountsEnabled").asBoolean()).isFalse();
        assertThat(projected.path("redirectUris")).isEmpty();
        assertThat(projected.path("attributes").path("identityhub.audience").asString())
                .isEqualTo("catalog-api");
    }

    @Test
    void registersPendingLocalIdentityUsingIsolatedServiceAccount() throws Exception {
        var registrar = new KeycloakLocalIdentityRegistrar(
                HttpClient.newHttpClient(),
                JSON,
                keycloakBaseUri(),
                REALM,
                IDENTITY_MANAGEMENT_CLIENT_ID,
                IDENTITY_MANAGEMENT_CLIENT_SECRET);

        var email = new LoginEmail("Pending.User@Example.test");
        UUID createdUserId;
        try (var password = new LocalPassword("test-only-long-local-password".toCharArray())) {
            var first = registrar.register(new PendingLocalIdentity(email, password));
            var replay = registrar.register(new PendingLocalIdentity(email, password));

            assertThat(first.created()).isTrue();
            assertThat(replay.created()).isFalse();
            assertThat(replay.userAccountRef()).isEqualTo(first.userAccountRef());
            createdUserId = first.userAccountRef().value();
        }

        var bootstrapToken = requestBootstrapAdminToken();
        var lookup = httpClient(HttpClient.Redirect.NORMAL).send(
                authorizedGetRequest(
                        keycloakBaseUri.resolve(
                                "/admin/realms/" + REALM
                                        + "/users?username=pending.user%40example.test&exact=true"),
                        bootstrapToken),
                HttpResponse.BodyHandlers.ofString());
        assertThat(lookup.statusCode()).isEqualTo(200);
        var stored = JSON.readTree(lookup.body()).required(0);
        assertThat(stored.required("id").asString())
                .isEqualTo(createdUserId.toString());
        assertThat(stored.path("username").asString()).isEqualTo("pending.user@example.test");
        assertThat(stored.path("email").asString()).isEqualTo("pending.user@example.test");
        assertThat(stored.path("enabled").asBoolean()).isFalse();
        assertThat(stored.path("emailVerified").asBoolean()).isFalse();

        registrar.verifyAndEnable(
                new UserAccountRef(createdUserId),
                new LoginEmail("pending.user@example.test"));
        registrar.verifyAndEnable(
                new UserAccountRef(createdUserId),
                new LoginEmail("pending.user@example.test"));
        var verifiedLookup = httpClient(HttpClient.Redirect.NORMAL).send(
                authorizedGetRequest(
                        keycloakBaseUri.resolve(
                                "/admin/realms/" + REALM
                                        + "/users?username=pending.user%40example.test&exact=true"),
                        bootstrapToken),
                HttpResponse.BodyHandlers.ofString());
        var verified = JSON.readTree(verifiedLookup.body()).required(0);
        assertThat(verified.path("enabled").asBoolean()).isTrue();
        assertThat(verified.path("emailVerified").asBoolean()).isTrue();

        var identityToken = requestServiceAccountToken(
                IDENTITY_MANAGEMENT_CLIENT_ID, IDENTITY_MANAGEMENT_CLIENT_SECRET);
        var forbiddenClients = httpClient(HttpClient.Redirect.NORMAL).send(
                authorizedGetRequest(
                        keycloakBaseUri.resolve("/admin/realms/" + REALM + "/clients"),
                        identityToken),
                HttpResponse.BodyHandlers.discarding());
        assertThat(forbiddenClients.statusCode()).isEqualTo(403);

        var clientManagementToken = requestServiceAccountToken(
                MANAGEMENT_CLIENT_ID, MANAGEMENT_CLIENT_SECRET);
        var forbiddenUsers = httpClient(HttpClient.Redirect.NORMAL).send(
                authorizedGetRequest(
                        keycloakBaseUri.resolve("/admin/realms/" + REALM + "/users"),
                        clientManagementToken),
                HttpResponse.BodyHandlers.discarding());
        assertThat(forbiddenUsers.statusCode()).isEqualTo(403);
    }

    @Test
    void projectsPublicSpaWithAuthorizationCodeAndPkceS256() throws Exception {
        var clientId = UUID.fromString("b4f65b7d-25ab-46ce-a44a-63ebca01d810");
        var redirectUri = "http://127.0.0.1:5173/auth/callback";
        var webOrigin = "http://127.0.0.1:5173";
        var snapshot = new ApplicationClientSnapshot(
                clientId,
                UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0"),
                "catalog-web",
                "SPA",
                null,
                List.of(redirectUri),
                List.of(webOrigin),
                true,
                Instant.parse("2026-08-01T12:00:00Z"),
                UUID.fromString("4fef31b8-17db-40d8-af99-e2899b7db57c"),
                1,
                "keycloak-spa-integration-test",
                ApplicationClientProjectionState.PENDING,
                0,
                Instant.parse("2026-08-01T12:00:00Z"),
                null);
        var projector = new KeycloakApplicationClientProjector(
                HttpClient.newHttpClient(),
                JSON,
                keycloakBaseUri,
                REALM,
                MANAGEMENT_CLIENT_ID,
                MANAGEMENT_CLIENT_SECRET);

        projector.project(snapshot);
        projector.project(snapshot);

        var projected = findKeycloakClient("ih-spa-" + clientId);
        assertThat(projected.path("publicClient").asBoolean()).isTrue();
        assertThat(projected.path("bearerOnly").asBoolean()).isFalse();
        assertThat(projected.path("standardFlowEnabled").asBoolean()).isTrue();
        assertThat(projected.path("implicitFlowEnabled").asBoolean()).isFalse();
        assertThat(projected.path("directAccessGrantsEnabled").asBoolean()).isFalse();
        assertThat(projected.path("serviceAccountsEnabled").asBoolean()).isFalse();
        assertThat(projected.path("authorizationServicesEnabled").asBoolean()).isFalse();
        assertThat(projected.path("redirectUris"))
                .containsExactly(JSON.valueToTree(redirectUri));
        assertThat(projected.path("webOrigins"))
                .containsExactly(JSON.valueToTree(webOrigin));
        assertThat(projected.path("attributes").path("pkce.code.challenge.method").asString())
                .isEqualTo("S256");
        assertThat(projected.path("attributes").path("identityhub.audience").isMissingNode())
                .isTrue();
    }

    @Test
    void enforcesLocalLoginSecurityBaselineInRealKeycloak() throws Exception {
        var response = httpClient(HttpClient.Redirect.NORMAL).send(
                authorizedGetRequest(
                        keycloakBaseUri.resolve("/admin/realms/" + REALM),
                        requestBootstrapAdminToken()),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        var realm = JSON.readTree(response.body());
        assertThat(realm.path("bruteForceProtected").asBoolean()).isTrue();
        assertThat(realm.path("permanentLockout").asBoolean()).isFalse();
        assertThat(realm.path("failureFactor").asInt()).isEqualTo(5);
        assertThat(realm.path("quickLoginCheckMilliSeconds").asInt()).isZero();
        assertThat(realm.path("waitIncrementSeconds").asInt()).isEqualTo(30);
        assertThat(realm.path("maxFailureWaitSeconds").asInt()).isEqualTo(900);
        assertThat(realm.path("eventsEnabled").asBoolean()).isTrue();
        assertThat(realm.path("enabledEventTypes"))
                .containsExactlyInAnyOrder(
                        JSON.valueToTree("LOGIN"),
                        JSON.valueToTree("LOGIN_ERROR"));
    }

    @Test
    void authenticatesVerifiedLocalIdentityThroughHostedOidcWithoutBusinessAccess()
            throws Exception {
        projectLocalLoginSpa();
        var userAccountRef = registerVerifiedLocalLoginUser();

        var tokens = authenticateLocalUser(
                "ih-spa-" + LOCAL_LOGIN_CLIENT_ID,
                LOCAL_LOGIN_REDIRECT_URI,
                LOCAL_LOGIN_USERNAME,
                LOCAL_LOGIN_PASSWORD,
                LOCAL_LOGIN_PKCE_VERIFIER);
        var decoder = oidcDecoder();
        var idToken = decoder.decode(tokens.required("id_token").asString());
        var accessToken = decoder.decode(tokens.required("access_token").asString());

        assertThat(idToken.getSubject()).isEqualTo(userAccountRef.value().toString());
        assertThat(idToken.getAudience()).containsExactly("ih-spa-" + LOCAL_LOGIN_CLIENT_ID);
        assertThat(accessToken.getAudience()).isNullOrEmpty();
        assertThat(accessToken.getClaims()).doesNotContainKey("resource_access");

        var eventsResponse = httpClient(HttpClient.Redirect.NORMAL).send(
                authorizedGetRequest(
                        keycloakBaseUri.resolve(
                                "/admin/realms/" + REALM + "/events?client="
                                        + encode("ih-spa-" + LOCAL_LOGIN_CLIENT_ID)),
                        requestBootstrapAdminToken()),
                HttpResponse.BodyHandlers.ofString());
        assertThat(eventsResponse.statusCode()).isEqualTo(200);
        assertThat(eventsResponse.body()).contains("LOGIN");
        assertThat(eventsResponse.body()).doesNotContain(LOCAL_LOGIN_PASSWORD);
    }

    @Test
    void rejectsLocalLoginGenericallyAndActivatesTemporaryBruteForceProtection()
            throws Exception {
        projectLocalLoginSpa();
        var userAccountRef = registerVerifiedLocalLoginUser(
                BRUTE_FORCE_USERNAME,
                BRUTE_FORCE_PASSWORD);
        registerPendingLocalLoginUser(
                "disabled.user@example.test",
                "test-only-disabled-user-password");
        var clientId = "ih-spa-" + LOCAL_LOGIN_CLIENT_ID;

        var wrongPassword = attemptLocalLogin(
                clientId,
                LOCAL_LOGIN_REDIRECT_URI,
                BRUTE_FORCE_USERNAME,
                "test-only-wrong-password",
                LOCAL_LOGIN_PKCE_VERIFIER);
        var unknownAccount = attemptLocalLogin(
                clientId,
                LOCAL_LOGIN_REDIRECT_URI,
                "unknown.user@example.test",
                "test-only-wrong-password",
                LOCAL_LOGIN_PKCE_VERIFIER);
        var disabledAccount = attemptLocalLogin(
                clientId,
                LOCAL_LOGIN_REDIRECT_URI,
                "disabled.user@example.test",
                "test-only-disabled-user-password",
                LOCAL_LOGIN_PKCE_VERIFIER);

        assertGenericLoginFailure(wrongPassword);
        assertGenericLoginFailure(unknownAccount);
        assertGenericLoginFailure(disabledAccount);
        for (var attempt = 1; attempt < 5; attempt++) {
            assertGenericLoginFailure(attemptLocalLogin(
                    clientId,
                    LOCAL_LOGIN_REDIRECT_URI,
                    BRUTE_FORCE_USERNAME,
                    "test-only-wrong-password-" + attempt,
                    LOCAL_LOGIN_PKCE_VERIFIER));
        }

        var bruteForceStatus = httpClient(HttpClient.Redirect.NORMAL).send(
                authorizedGetRequest(
                        keycloakBaseUri.resolve(
                                "/admin/realms/" + REALM
                                        + "/attack-detection/brute-force/users/"
                                        + userAccountRef.value()),
                        requestBootstrapAdminToken()),
                HttpResponse.BodyHandlers.ofString());
        assertThat(bruteForceStatus.statusCode()).isEqualTo(200);
        var detection = JSON.readTree(bruteForceStatus.body());
        assertThat(detection.path("numFailures").asInt()).isGreaterThanOrEqualTo(5);
        assertThat(detection.path("disabled").asBoolean()).isTrue();

        var eventsResponse = httpClient(HttpClient.Redirect.NORMAL).send(
                authorizedGetRequest(
                        keycloakBaseUri.resolve(
                                "/admin/realms/" + REALM + "/events?client="
                                        + encode(clientId)),
                        requestBootstrapAdminToken()),
                HttpResponse.BodyHandlers.ofString());
        assertThat(eventsResponse.statusCode()).isEqualTo(200);
        var serializedEvents = eventsResponse.body();
        assertThat(serializedEvents).contains("LOGIN_ERROR");
        assertThat(serializedEvents)
                .doesNotContain("test-only-wrong-password", BRUTE_FORCE_PASSWORD);
    }

    private static void projectLocalLoginSpa() {
        var snapshot = new ApplicationClientSnapshot(
                LOCAL_LOGIN_CLIENT_ID,
                UUID.fromString("ee551ec6-5f78-45ed-bd13-f02becf0fc62"),
                "local-login-spa",
                "SPA",
                null,
                List.of(LOCAL_LOGIN_REDIRECT_URI),
                List.of("http://127.0.0.1:5173"),
                true,
                Instant.parse("2026-08-02T12:00:00Z"),
                UUID.fromString("553738ed-8d46-46bc-ac3a-2af435a14ac4"),
                1,
                "hosted-local-login-test",
                ApplicationClientProjectionState.PENDING,
                0,
                Instant.parse("2026-08-02T12:00:00Z"),
                null);
        new KeycloakApplicationClientProjector(
                        HttpClient.newHttpClient(),
                        JSON,
                        keycloakBaseUri,
                        REALM,
                        MANAGEMENT_CLIENT_ID,
                        MANAGEMENT_CLIENT_SECRET)
                .project(snapshot);
    }

    private static UserAccountRef registerVerifiedLocalLoginUser() {
        return registerVerifiedLocalLoginUser(LOCAL_LOGIN_USERNAME, LOCAL_LOGIN_PASSWORD);
    }

    private static UserAccountRef registerVerifiedLocalLoginUser(
            String username,
            String rawPassword) {
        var registration = registerPendingLocalLoginUser(
                username,
                rawPassword);
        var registrar = localIdentityRegistrar();
        registrar.verifyAndEnable(registration, new LoginEmail(username));
        return registration;
    }

    private static UserAccountRef registerPendingLocalLoginUser(
            String username,
            String rawPassword) {
        var registrar = localIdentityRegistrar();
        var email = new LoginEmail(username);
        LocalIdentityRegistration registration;
        try (var password = new LocalPassword(rawPassword.toCharArray())) {
            registration = registrar.register(new PendingLocalIdentity(email, password));
        }
        return registration.userAccountRef();
    }

    private static KeycloakLocalIdentityRegistrar localIdentityRegistrar() {
        return new KeycloakLocalIdentityRegistrar(
                HttpClient.newHttpClient(),
                JSON,
                keycloakBaseUri(),
                REALM,
                IDENTITY_MANAGEMENT_CLIENT_ID,
                IDENTITY_MANAGEMENT_CLIENT_SECRET);
    }

    private static JsonNode authenticateLocalUser(
            String clientId,
            String redirectUri,
            String username,
            String password,
            String verifier) throws Exception {
        var client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        var loginResponse = attemptLocalLogin(
                clientId,
                redirectUri,
                username,
                password,
                verifier);
        assertThat(loginResponse.statusCode()).isEqualTo(302);
        var location = loginResponse.headers().firstValue("Location").orElseThrow();
        assertThat(location)
                .describedAs("Hosted local login redirect")
                .contains("code=");
        assertThat(queryParameter(location, "state")).isEqualTo("local-login-state");
        var code = queryParameter(location, "code");

        var tokenResponse = client.send(
                formRequest(
                        keycloakBaseUri.resolve(
                                "/realms/" + REALM + "/protocol/openid-connect/token"),
                        Map.of(
                                "grant_type", "authorization_code",
                                "client_id", clientId,
                                "redirect_uri", redirectUri,
                                "code_verifier", verifier,
                                "code", code)),
                HttpResponse.BodyHandlers.ofString());
        assertThat(tokenResponse.statusCode()).isEqualTo(200);
        return JSON.readTree(tokenResponse.body());
    }

    private static HttpResponse<String> attemptLocalLogin(
            String clientId,
            String redirectUri,
            String username,
            String password,
            String verifier) throws Exception {
        var client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        var authorizationUri = keycloakBaseUri.resolve(
                "/realms/" + REALM + "/protocol/openid-connect/auth"
                        + "?client_id=" + encode(clientId)
                        + "&response_type=code"
                        + "&scope=openid"
                        + "&state=local-login-state"
                        + "&nonce=local-login-nonce"
                        + "&code_challenge_method=S256"
                        + "&code_challenge=" + encode(pkceChallenge(verifier))
                        + "&redirect_uri=" + encode(redirectUri));
        var loginPage = client.send(
                HttpRequest.newBuilder(authorizationUri).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(loginPage.statusCode()).isEqualTo(200);

        return client.send(
                formRequest(
                        formAction(loginPage.body()),
                        Map.of(
                                "username", username,
                                "password", password,
                                "credentialId", ""),
                        cookies(loginPage)),
                HttpResponse.BodyHandlers.ofString());
    }

    private static void assertGenericLoginFailure(HttpResponse<String> response) {
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Invalid username or password.");
        assertThat(response.headers().firstValue("Location")).isEmpty();
    }

    private static NimbusJwtDecoder oidcDecoder() {
        var decoder = NimbusJwtDecoder.withJwkSetUri(
                        realmIssuer() + "/protocol/openid-connect/certs")
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(realmIssuer().toString()));
        return decoder;
    }

    private static JsonNode findKeycloakClient(String clientId) throws Exception {
        var response = httpClient(HttpClient.Redirect.NORMAL).send(
                authorizedGetRequest(
                        keycloakBaseUri.resolve(
                                "/admin/realms/" + REALM + "/clients?clientId="
                                        + clientId + "&exact=true"),
                        requestBootstrapAdminToken()),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        var clients = JSON.readTree(response.body());
        assertThat(clients).hasSize(1);
        return clients.required(0);
    }

    private void assertClientApplicationAdministration(String accessToken)
            throws Exception {
        var applicationId = UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0");
        var applicationUri = URI.create("http://127.0.0.1:" + servicePort
                + "/internal/admin/client-applications/" + applicationId);
        var correlationId = "real-keycloak-client-application";
        var requestBody = """
                {
                  "identifier": "auto-radar",
                  "displayName": "Auto Radar"
                }
                """;

        var registration = HttpClient.newHttpClient().send(
                authorizedPutJsonRequest(
                        applicationUri,
                        accessToken,
                        correlationId,
                        requestBody),
                HttpResponse.BodyHandlers.ofString());

        assertThat(registration.statusCode()).isEqualTo(201);
        assertThat(registration.body()).contains(
                "\"identifier\":\"auto-radar\"",
                "\"state\":\"DRAFT\"");

        var lookup = HttpClient.newHttpClient().send(
                authorizedGetRequest(applicationUri, accessToken),
                HttpResponse.BodyHandlers.ofString());

        assertThat(lookup.statusCode()).isEqualTo(200);
        assertThat(lookup.body()).contains("\"applicationId\":\"" + applicationId + "\"");
        assertThat(jdbcClient.sql("select count(*) from client_application")
                .query(Integer.class)
                .single())
                .isEqualTo(1);
        assertThat(jdbcClient.sql("""
                            select outcome
                            from administrative_access_event
                            where correlation_id = :correlationId
                            """)
                .param("correlationId", correlationId)
                .query(String.class)
                .single())
                .isEqualTo("ALLOWED");

        assertProtectedApiProjection(accessToken, applicationUri, applicationId);
        assertSpaProjection(accessToken, applicationUri, applicationId);
        assertBffProjectionAndSecretRotation(accessToken, applicationUri, applicationId);
        assertMachineProjectionAndSecretIssuance(accessToken, applicationUri, applicationId);
    }

    private void assertProtectedApiProjection(
            String accessToken,
            URI applicationUri,
            UUID applicationId) throws Exception {
        var clientId = UUID.fromString("1ae43ab3-ad03-41fe-b734-e579824e0e93");
        var clientUri = URI.create(applicationUri + "/clients/" + clientId);
        var response = HttpClient.newHttpClient().send(
                authorizedPutJsonRequest(
                        clientUri,
                        accessToken,
                        "configure-real-protected-api",
                        """
                                {
                                  "type": "API",
                                  "key": "real-protected-api",
                                  "audience": "real-protected-api"
                                }
                                """),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).contains(
                "\"applicationId\":\"" + applicationId + "\"",
                "\"projectionState\":\"PENDING\"");

        awaitAppliedProjection(clientId);

        var lookup = HttpClient.newHttpClient().send(
                authorizedGetRequest(clientUri, accessToken),
                HttpResponse.BodyHandlers.ofString());
        assertThat(lookup.statusCode()).isEqualTo(200);
        assertThat(lookup.body()).contains("\"projectionState\":\"APPLIED\"");
    }

    private void assertSpaProjection(
            String accessToken,
            URI applicationUri,
            UUID applicationId) throws Exception {
        var clientId = UUID.fromString("72c43df3-9f34-4dc6-85cc-5d323762f299");
        var clientUri = URI.create(applicationUri + "/clients/" + clientId);
        var response = HttpClient.newHttpClient().send(
                authorizedPutJsonRequest(
                        clientUri,
                        accessToken,
                        "configure-real-spa",
                        """
                                {
                                  "type": "SPA",
                                  "key": "real-public-spa",
                                  "redirectUris": [
                                    "http://127.0.0.1:5173/auth/callback"
                                  ],
                                  "webOrigins": ["http://127.0.0.1:5173"]
                                }
                                """),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).contains(
                "\"applicationId\":\"" + applicationId + "\"",
                "\"type\":\"SPA\"",
                "\"projectionState\":\"PENDING\"");

        awaitAppliedProjection(clientId);

        var lookup = HttpClient.newHttpClient().send(
                authorizedGetRequest(clientUri, accessToken),
                HttpResponse.BodyHandlers.ofString());
        assertThat(lookup.statusCode()).isEqualTo(200);
        assertThat(lookup.body()).contains("\"projectionState\":\"APPLIED\"");
        var projected = findKeycloakClient("ih-spa-" + clientId);
        assertThat(projected.path("publicClient").asBoolean()).isTrue();
        assertThat(projected.path("standardFlowEnabled").asBoolean()).isTrue();
        assertThat(projected.path("attributes").path("pkce.code.challenge.method").asString())
                .isEqualTo("S256");
    }

    private void assertBffProjectionAndSecretRotation(
            String accessToken,
            URI applicationUri,
            UUID applicationId) throws Exception {
        var clientId = UUID.fromString("b99a9298-13ac-45b4-b198-19908e190f10");
        var clientUri = URI.create(applicationUri + "/clients/" + clientId);
        var response = HttpClient.newHttpClient().send(
                authorizedPutJsonRequest(
                        clientUri,
                        accessToken,
                        "configure-real-bff",
                        """
                                {
                                  "type": "BFF",
                                  "key": "real-confidential-bff",
                                  "redirectUris": [
                                    "http://127.0.0.1:8081/login/oauth2/code/identityhub"
                                  ]
                                }
                                """),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).contains(
                "\"applicationId\":\"" + applicationId + "\"",
                "\"type\":\"BFF\"",
                "\"projectionState\":\"PENDING\"");

        awaitAppliedProjection(clientId);

        var projected = findKeycloakClient("ih-bff-" + clientId);
        assertThat(projected.path("publicClient").asBoolean()).isFalse();
        assertThat(projected.path("bearerOnly").asBoolean()).isFalse();
        assertThat(projected.path("clientAuthenticatorType").asString())
                .isEqualTo("client-secret");
        assertThat(projected.path("standardFlowEnabled").asBoolean()).isTrue();
        assertThat(projected.path("serviceAccountsEnabled").asBoolean()).isFalse();
        assertThat(projected.path("attributes").path("pkce.code.challenge.method").asString())
                .isEqualTo("S256");

        var secretUri = URI.create(clientUri + "/credentials/client-secret");
        var firstRotation = HttpClient.newHttpClient().send(
                authorizedPostRequest(secretUri, accessToken, "rotate-real-bff-secret-1"),
                HttpResponse.BodyHandlers.ofString());
        assertThat(firstRotation.statusCode()).isEqualTo(200);
        assertThat(firstRotation.headers().firstValue("Cache-Control"))
                .contains("no-store");
        var firstSecret = JSON.readTree(firstRotation.body())
                .required("clientSecret")
                .asString();
        assertThat(firstSecret).isNotBlank();

        var secondRotation = HttpClient.newHttpClient().send(
                authorizedPostRequest(secretUri, accessToken, "rotate-real-bff-secret-2"),
                HttpResponse.BodyHandlers.ofString());
        assertThat(secondRotation.statusCode()).isEqualTo(200);
        var secondSecret = JSON.readTree(secondRotation.body())
                .required("clientSecret")
                .asString();
        assertThat(secondSecret).isNotBlank();
        assertThat(MessageDigest.isEqual(
                        MessageDigest.getInstance("SHA-256")
                                .digest(firstSecret.getBytes(StandardCharsets.UTF_8)),
                        MessageDigest.getInstance("SHA-256")
                                .digest(secondSecret.getBytes(StandardCharsets.UTF_8))))
                .isFalse();

        var reconciliation = HttpClient.newHttpClient().send(
                authorizedPostRequest(
                        URI.create(clientUri + "/projection/reconcile"),
                        accessToken,
                        "reconcile-real-bff-after-secret"),
                HttpResponse.BodyHandlers.ofString());
        assertThat(reconciliation.statusCode()).isEqualTo(202);
        awaitAppliedProjection(clientId);

        var retainedSecret = currentKeycloakClientSecret(projected.required("id").asString());
        assertThat(MessageDigest.isEqual(
                        MessageDigest.getInstance("SHA-256")
                                .digest(secondSecret.getBytes(StandardCharsets.UTF_8)),
                        MessageDigest.getInstance("SHA-256")
                                .digest(retainedSecret.getBytes(StandardCharsets.UTF_8))))
                .isTrue();
    }

    private static String currentKeycloakClientSecret(String keycloakClientId) throws Exception {
        var response = httpClient(HttpClient.Redirect.NORMAL).send(
                authorizedGetRequest(
                        keycloakBaseUri.resolve(
                                "/admin/realms/" + REALM + "/clients/" + keycloakClientId
                                        + "/client-secret"),
                        requestBootstrapAdminToken()),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return JSON.readTree(response.body()).required("value").asString();
    }

    private void assertMachineProjectionAndSecretIssuance(
            String accessToken,
            URI applicationUri,
            UUID applicationId) throws Exception {
        var clientId = UUID.fromString("04f4fc41-3ff0-42bb-91b6-76fc48d744b0");
        var clientUri = URI.create(applicationUri + "/clients/" + clientId);
        var response = HttpClient.newHttpClient().send(
                authorizedPutJsonRequest(
                        clientUri,
                        accessToken,
                        "configure-real-machine",
                        """
                                {
                                  "type": "MACHINE",
                                  "key": "real-membership-provisioner"
                                }
                                """),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).contains(
                "\"applicationId\":\"" + applicationId + "\"",
                "\"type\":\"MACHINE\"",
                "\"projectionState\":\"PENDING\"");

        awaitAppliedProjection(clientId);

        var projected = findKeycloakClient("ih-machine-" + clientId);
        assertThat(projected.path("publicClient").asBoolean()).isFalse();
        assertThat(projected.path("bearerOnly").asBoolean()).isFalse();
        assertThat(projected.path("clientAuthenticatorType").asString())
                .isEqualTo("client-secret");
        assertThat(projected.path("standardFlowEnabled").asBoolean()).isFalse();
        assertThat(projected.path("serviceAccountsEnabled").asBoolean()).isTrue();
        assertThat(projected.path("directAccessGrantsEnabled").asBoolean()).isFalse();
        assertThat(projected.path("redirectUris")).isEmpty();
        assertThat(projected.path("webOrigins")).isEmpty();

        var secretResponse = HttpClient.newHttpClient().send(
                authorizedPostRequest(
                        URI.create(clientUri + "/credentials/client-secret"),
                        accessToken,
                        "issue-real-machine-secret"),
                HttpResponse.BodyHandlers.ofString());
        assertThat(secretResponse.statusCode()).isEqualTo(200);
        assertThat(secretResponse.headers().firstValue("Cache-Control")).contains("no-store");
        assertThat(JSON.readTree(secretResponse.body()).required("clientSecret").asString())
                .isNotBlank();
    }

    private void awaitAppliedProjection(UUID clientId) throws InterruptedException {
        var deadline = Instant.now().plusSeconds(30);
        String state;
        do {
            state = jdbcClient.sql("""
                            select state
                            from application_client_projection_outbox
                            where application_client_id = :clientId
                            """)
                    .param("clientId", clientId)
                    .query(String.class)
                    .single();
            if (!state.equals("PENDING")) {
                break;
            }
            Thread.sleep(200);
        } while (Instant.now().isBefore(deadline));
        assertThat(state).isEqualTo("APPLIED");
    }

    private static void assertTokenIsolation(String accessToken, URI issuer) {
        var jwkSetUri = URI.create(issuer + "/protocol/openid-connect/certs");
        var otherEnvironment = new AdminSecurityProperties(
                URI.create(issuer.toString().replace(REALM, "another-environment")),
                jwkSetUri,
                ADMIN_AUDIENCE);
        var anotherAudience = new AdminSecurityProperties(
                issuer,
                jwkSetUri,
                "another-admin-api");

        assertThatThrownBy(() -> new AdminSecurityConfiguration()
                        .adminJwtDecoder(otherEnvironment)
                        .decode(accessToken))
                .isInstanceOf(JwtValidationException.class);
        assertThatThrownBy(() -> new AdminSecurityConfiguration()
                        .adminJwtDecoder(anotherAudience)
                        .decode(accessToken))
                .isInstanceOf(JwtValidationException.class);
    }

    private static URI keycloakBaseUri() {
        return URI.create(
                "http://" + KEYCLOAK.getHost() + ":" + KEYCLOAK.getMappedPort(8080));
    }

    private static URI realmIssuer() {
        return keycloakBaseUri().resolve("/realms/" + REALM);
    }

    private static String syntheticPassword() {
        return "test-only-" + UUID.randomUUID();
    }

    private static String requestBootstrapAdminToken() throws Exception {
        var response = httpClient(HttpClient.Redirect.NORMAL).send(
                formRequest(
                        keycloakBaseUri.resolve("/realms/master/protocol/openid-connect/token"),
                        Map.of(
                                "grant_type", "password",
                                "client_id", "admin-cli",
                                "username", BOOTSTRAP_ADMIN,
                                "password", BOOTSTRAP_PASSWORD)),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return JSON.readTree(response.body()).required("access_token").asString();
    }

    private static String requestServiceAccountToken(String clientId, String clientSecret)
            throws Exception {
        var response = httpClient(HttpClient.Redirect.NORMAL).send(
                formRequest(
                        keycloakBaseUri.resolve(
                                "/realms/" + REALM + "/protocol/openid-connect/token"),
                        Map.of(
                                "grant_type", "client_credentials",
                                "client_id", clientId,
                                "client_secret", clientSecret)),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return JSON.readTree(response.body()).required("access_token").asString();
    }

    private static void createRealm(String adminToken) throws Exception {
        var response = httpClient(HttpClient.Redirect.NORMAL).send(
                authorizedJsonRequest(
                        keycloakBaseUri.resolve("/admin/realms"),
                        adminToken,
                        realmRepresentation()),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode())
                .describedAs("Keycloak realm creation response: %s", response.body())
                .isEqualTo(201);
    }

    private static void configureAuthenticationMethodReferences(String adminToken)
            throws Exception {
        var executionsUri = keycloakBaseUri.resolve(
                "/admin/realms/" + REALM + "/authentication/flows/browser/executions");
        var response = httpClient(HttpClient.Redirect.NORMAL).send(
                authorizedGetRequest(executionsUri, adminToken),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);

        var references = Map.of(
                "auth-username-password-form", "pwd",
                "auth-conditional-otp-form", "totp",
                "auth-otp-form", "totp");
        var configuredReferences = 0;
        for (JsonNode execution : JSON.readTree(response.body())) {
            var reference = references.get(execution.path("providerId").asString());
            if (reference != null) {
                configureAuthenticationMethodReference(
                        adminToken,
                        execution.required("id").asString(),
                        reference);
                configuredReferences++;
            }
        }
        assertThat(configuredReferences).isGreaterThanOrEqualTo(2);
    }

    private static void grantClientManagement(String adminToken) throws Exception {
        grantManagementRoles(adminToken, MANAGEMENT_CLIENT_ID, List.of("manage-clients"));
    }

    private static void grantIdentityManagement(String adminToken) throws Exception {
        grantManagementRoles(
                adminToken,
                IDENTITY_MANAGEMENT_CLIENT_ID,
                List.of("manage-users", "view-users", "query-users"));
    }

    private static void grantManagementRoles(
            String adminToken, String serviceClientId, List<String> roleNames)
            throws Exception {
        var managementClientUuid = clientUuid(adminToken, serviceClientId);
        var realmManagementUuid = clientUuid(adminToken, "realm-management");
        var serviceAccountResponse = httpClient(HttpClient.Redirect.NORMAL).send(
                authorizedGetRequest(
                        keycloakBaseUri.resolve(
                                "/admin/realms/" + REALM + "/clients/"
                                        + managementClientUuid + "/service-account-user"),
                        adminToken),
                HttpResponse.BodyHandlers.ofString());
        assertThat(serviceAccountResponse.statusCode()).isEqualTo(200);
        var serviceAccountId = JSON.readTree(serviceAccountResponse.body())
                .required("id")
                .asString();

        var roles = new java.util.ArrayList<JsonNode>();
        for (var roleName : roleNames) {
            var roleResponse = httpClient(HttpClient.Redirect.NORMAL).send(
                    authorizedGetRequest(
                            keycloakBaseUri.resolve(
                                    "/admin/realms/" + REALM + "/clients/"
                                            + realmManagementUuid + "/roles/" + roleName),
                            adminToken),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(roleResponse.statusCode()).isEqualTo(200);
            roles.add(JSON.readTree(roleResponse.body()));
        }
        var roleMapping = JSON.writeValueAsString(roles);

        var mappingResponse = httpClient(HttpClient.Redirect.NORMAL).send(
                authorizedJsonRequest(
                        keycloakBaseUri.resolve(
                                "/admin/realms/" + REALM + "/users/" + serviceAccountId
                                        + "/role-mappings/clients/" + realmManagementUuid),
                        adminToken,
                        roleMapping),
                HttpResponse.BodyHandlers.discarding());
        assertThat(mappingResponse.statusCode()).isEqualTo(204);

        var scopeMappingResponse = httpClient(HttpClient.Redirect.NORMAL).send(
                authorizedJsonRequest(
                        keycloakBaseUri.resolve(
                                "/admin/realms/" + REALM + "/clients/" + managementClientUuid
                                        + "/scope-mappings/clients/" + realmManagementUuid),
                        adminToken,
                        roleMapping),
                HttpResponse.BodyHandlers.discarding());
        assertThat(scopeMappingResponse.statusCode()).isEqualTo(204);
    }

    private static String clientUuid(String adminToken, String clientId) throws Exception {
        var response = httpClient(HttpClient.Redirect.NORMAL).send(
                authorizedGetRequest(
                        keycloakBaseUri.resolve(
                                "/admin/realms/" + REALM + "/clients?clientId="
                                        + encode(clientId) + "&exact=true"),
                        adminToken),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return JSON.readTree(response.body()).required(0).required("id").asString();
    }

    private static void configureAuthenticationMethodReference(
            String adminToken,
            String executionId,
            String reference) throws Exception {
        var body = JSON.writeValueAsString(Map.of(
                "alias", "identityhub-amr-" + reference,
                "config", Map.of(
                        "default.reference.value", reference,
                        "default.reference.maxAge", "300")));
        var uri = keycloakBaseUri.resolve(
                "/admin/realms/" + REALM
                        + "/authentication/executions/" + executionId + "/config");
        var response = httpClient(HttpClient.Redirect.NORMAL).send(
                authorizedJsonRequest(uri, adminToken, body),
                HttpResponse.BodyHandlers.discarding());
        assertThat(response.statusCode()).isEqualTo(201);
    }

    private static String authenticateThroughHostedLogin() throws Exception {
        var client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        var authorizationUri = keycloakBaseUri.resolve(
                "/realms/" + REALM + "/protocol/openid-connect/auth"
                        + "?client_id=" + encode(CLIENT_ID)
                        + "&response_type=code"
                        + "&scope=openid"
                        + "&code_challenge_method=S256"
                        + "&code_challenge=" + encode(pkceChallenge())
                        + "&redirect_uri=" + encode(REDIRECT_URI));
        var loginPage = client.send(
                HttpRequest.newBuilder(authorizationUri).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(loginPage.statusCode()).isEqualTo(200);
        var loginCookies = cookies(loginPage);

        var passwordResponse = client.send(
                formRequest(
                        formAction(loginPage.body()),
                        Map.of(
                                "username", USERNAME,
                                "password", PASSWORD,
                                "credentialId", ""),
                        loginCookies),
                HttpResponse.BodyHandlers.ofString());
        assertThat(passwordResponse.statusCode())
                .describedAs("Keycloak password form response")
                .isEqualTo(200);

        var otpResponse = client.send(
                formRequest(
                        formAction(passwordResponse.body()),
                        Map.of("otp", currentTotp()),
                        mergeCookies(loginCookies, cookies(passwordResponse))),
                HttpResponse.BodyHandlers.ofString());
        assertThat(otpResponse.statusCode()).isEqualTo(302);

        var authorizationCode = queryParameter(
                otpResponse.headers().firstValue("Location").orElseThrow(),
                "code");
        var tokenResponse = client.send(
                formRequest(
                        keycloakBaseUri.resolve(
                                "/realms/" + REALM + "/protocol/openid-connect/token"),
                        Map.of(
                                "grant_type", "authorization_code",
                                "client_id", CLIENT_ID,
                                "redirect_uri", REDIRECT_URI,
                                "code_verifier", PKCE_VERIFIER,
                                "code", authorizationCode)),
                HttpResponse.BodyHandlers.ofString());
        assertThat(tokenResponse.statusCode()).isEqualTo(200);
        return JSON.readTree(tokenResponse.body()).required("access_token").asString();
    }

    private static HttpRequest authorizedGetRequest(URI uri, String bearerToken) {
        return HttpRequest.newBuilder(uri)
                .header("Authorization", "Bearer " + bearerToken)
                .GET()
                .build();
    }

    private static HttpRequest authorizedJsonRequest(
            URI uri,
            String bearerToken,
            String body) {
        return HttpRequest.newBuilder(uri)
                .header("Authorization", "Bearer " + bearerToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private static HttpRequest authorizedPutJsonRequest(
            URI uri,
            String bearerToken,
            String correlationId,
            String body) {
        return HttpRequest.newBuilder(uri)
                .header("Authorization", "Bearer " + bearerToken)
                .header("X-Correlation-ID", correlationId)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private static HttpRequest authorizedPostRequest(
            URI uri,
            String bearerToken,
            String correlationId) {
        return HttpRequest.newBuilder(uri)
                .header("Authorization", "Bearer " + bearerToken)
                .header("X-Correlation-ID", correlationId)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
    }

    private static HttpRequest formRequest(URI uri, Map<String, String> fields) {
        return formRequest(uri, fields, null);
    }

    private static HttpRequest formRequest(
            URI uri,
            Map<String, String> fields,
            String cookies) {
        var body = fields.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .sorted()
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
        var request = HttpRequest.newBuilder(uri)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (cookies != null && !cookies.isBlank()) {
            request.header("Cookie", cookies);
        }
        return request.build();
    }

    private static HttpClient httpClient(HttpClient.Redirect redirects) {
        return HttpClient.newBuilder().followRedirects(redirects).build();
    }

    private static URI formAction(String html) {
        var matcher = FORM_ACTION.matcher(html);
        assertThat(matcher.find()).isTrue();
        var action = URI.create(matcher.group(1).replace("&amp;", "&"));
        var pathAndQuery = action.getRawPath()
                + (action.getRawQuery() == null ? "" : "?" + action.getRawQuery());
        return keycloakBaseUri.resolve(pathAndQuery);
    }

    private static String queryParameter(String location, String name) {
        return List.of(URI.create(location).getRawQuery().split("&")).stream()
                .map(parameter -> parameter.split("=", 2))
                .filter(parts -> parts.length == 2 && parts[0].equals(name))
                .map(parts -> java.net.URLDecoder.decode(parts[1], StandardCharsets.UTF_8))
                .findFirst()
                .orElseThrow();
    }

    private static String cookies(HttpResponse<?> response) {
        return mergeCookies(
                "",
                String.join(
                        "; ",
                        response.headers().allValues("Set-Cookie").stream()
                                .map(header -> header.split(";", 2)[0])
                                .toList()));
    }

    private static String mergeCookies(String existing, String replacement) {
        var values = new LinkedHashMap<String, String>();
        for (var header : List.of(existing, replacement)) {
            for (var cookie : header.split("; ")) {
                var parts = cookie.split("=", 2);
                if (parts.length == 2) {
                    values.put(parts[0], parts[1]);
                }
            }
        }
        return values.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + "; " + right)
                .orElse("");
    }

    private static String currentTotp() throws Exception {
        var counter = Instant.now().getEpochSecond() / 30;
        var mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(decodeBase32(OTP_SECRET), "HmacSHA1"));
        var hash = mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(counter).array());
        var offset = hash[hash.length - 1] & 0x0f;
        var binary = (hash[offset] & 0x7f) << 24
                | (hash[offset + 1] & 0xff) << 16
                | (hash[offset + 2] & 0xff) << 8
                | hash[offset + 3] & 0xff;
        return "%06d".formatted(binary % 1_000_000);
    }

    private static byte[] decodeBase32(String value) {
        var bits = new StringBuilder();
        for (char character : value.toCharArray()) {
            var index = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".indexOf(character);
            bits.append(String.format("%5s", Integer.toBinaryString(index)).replace(' ', '0'));
        }
        var result = new byte[bits.length() / Byte.SIZE];
        for (var index = 0; index < result.length; index++) {
            result[index] = (byte) Integer.parseInt(
                    bits.substring(index * Byte.SIZE, (index + 1) * Byte.SIZE),
                    2);
        }
        return result;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String pkceChallenge() throws Exception {
        return pkceChallenge(PKCE_VERIFIER);
    }

    private static void configureEmailOnlyUserProfile(String adminToken) throws Exception {
        var profileUri = keycloakBaseUri.resolve(
                "/admin/realms/" + REALM + "/users/profile");
        var lookup = httpClient(HttpClient.Redirect.NORMAL).send(
                authorizedGetRequest(profileUri, adminToken),
                HttpResponse.BodyHandlers.ofString());
        assertThat(lookup.statusCode()).isEqualTo(200);
        var profile = JSON.readTree(lookup.body());
        for (var attribute : profile.required("attributes")) {
            if (attribute instanceof ObjectNode object
                    && List.of("firstName", "lastName")
                            .contains(attribute.path("name").asString())) {
                object.remove("required");
            }
        }
        var update = httpClient(HttpClient.Redirect.NORMAL).send(
                authorizedPutJsonRequest(
                        profileUri,
                        adminToken,
                        "configure-email-only-profile",
                        JSON.writeValueAsString(profile)),
                HttpResponse.BodyHandlers.discarding());
        assertThat(update.statusCode()).isIn(200, 204);
    }

    private static void configureGenericLoginMessages(String adminToken) throws Exception {
        var messages = Map.of(
                "en", Map.of(
                        "accountDisabledMessage", "Invalid username or password.",
                        "accountTemporarilyDisabledMessage", "Invalid username or password."),
                "pt-BR", Map.of(
                        "accountDisabledMessage", "E-mail ou senha inválidos.",
                        "accountTemporarilyDisabledMessage", "E-mail ou senha inválidos."));
        for (var localized : messages.entrySet()) {
            for (var message : localized.getValue().entrySet()) {
                var uri = keycloakBaseUri.resolve(
                        "/admin/realms/" + REALM + "/localization/"
                                + encode(localized.getKey()) + "/" + message.getKey());
                var response = httpClient(HttpClient.Redirect.NORMAL).send(
                        HttpRequest.newBuilder(uri)
                                .header("Authorization", "Bearer " + adminToken)
                                .header("Content-Type", "text/plain; charset=UTF-8")
                                .PUT(HttpRequest.BodyPublishers.ofString(message.getValue()))
                                .build(),
                        HttpResponse.BodyHandlers.discarding());
                assertThat(response.statusCode()).isEqualTo(204);
            }
        }
    }

    private static String pkceChallenge(String verifier) throws Exception {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(MessageDigest.getInstance("SHA-256")
                        .digest(verifier.getBytes(StandardCharsets.US_ASCII)));
    }

    private static String realmRepresentation() {
        return """
                {
                  "realm": "%s",
                  "enabled": true,
                  "sslRequired": "none",
                  "otpPolicyType": "totp",
                  "otpPolicyAlgorithm": "HmacSHA1",
                  "otpPolicyDigits": 6,
                  "otpPolicyPeriod": 30,
                  "passwordPolicy": "length(15) and maxLength(64)",
                  "internationalizationEnabled": true,
                  "supportedLocales": ["en", "pt-BR"],
                  "defaultLocale": "en",
                  "bruteForceProtected": true,
                  "permanentLockout": false,
                  "failureFactor": 5,
                  "quickLoginCheckMilliSeconds": 0,
                  "waitIncrementSeconds": 30,
                  "maxFailureWaitSeconds": 900,
                  "maxDeltaTimeSeconds": 43200,
                  "eventsEnabled": true,
                  "eventsExpiration": 2592000,
                  "enabledEventTypes": ["LOGIN", "LOGIN_ERROR"],
                  "roles": {
                    "realm": [
                      {"name": "PLATFORM_ADMIN"},
                      {"name": "PLATFORM_AUDITOR"}
                    ]
                  },
                  "clients": [
                    {
                      "clientId": "%s",
                      "enabled": true,
                      "publicClient": false,
                      "clientAuthenticatorType": "client-secret",
                      "secret": "%s",
                      "standardFlowEnabled": false,
                      "directAccessGrantsEnabled": false,
                      "serviceAccountsEnabled": true,
                      "fullScopeAllowed": false
                    },
                    {
                      "clientId": "%s",
                      "enabled": true,
                      "publicClient": false,
                      "clientAuthenticatorType": "client-secret",
                      "secret": "%s",
                      "standardFlowEnabled": false,
                      "directAccessGrantsEnabled": false,
                      "serviceAccountsEnabled": true,
                      "fullScopeAllowed": false
                    },
                    {
                      "clientId": "%s",
                      "enabled": true,
                      "publicClient": true,
                      "standardFlowEnabled": true,
                      "directAccessGrantsEnabled": false,
                      "fullScopeAllowed": true,
                      "redirectUris": ["%s"],
                      "protocolMappers": [
                        {
                          "name": "identityhub-admin-audience",
                          "protocol": "openid-connect",
                          "protocolMapper": "oidc-audience-mapper",
                          "config": {
                            "included.custom.audience": "%s",
                            "access.token.claim": "true"
                          }
                        },
                        {
                          "name": "authentication-method-reference",
                          "protocol": "openid-connect",
                          "protocolMapper": "oidc-amr-mapper",
                          "config": {
                            "access.token.claim": "true",
                            "id.token.claim": "true"
                          }
                        }
                      ]
                    }
                  ],
                  "users": [
                    {
                      "username": "%s",
                      "enabled": true,
                      "email": "platform-admin@example.test",
                      "emailVerified": true,
                      "firstName": "Platform",
                      "lastName": "Administrator",
                      "realmRoles": ["PLATFORM_ADMIN"],
                      "credentials": [
                        {
                          "type": "password",
                          "value": "%s",
                          "temporary": false
                        },
                        {
                          "type": "otp",
                          "secretData": "{\\"value\\":\\"%s\\"}",
                          "credentialData": "{\\"subType\\":\\"totp\\",\\"digits\\":6,\
                \\"counter\\":0,\\"period\\":30,\\"algorithm\\":\\"HmacSHA1\\",\
                \\"secretEncoding\\":\\"BASE32\\"}"
                        }
                      ]
                    }
                  ]
                }
                """.formatted(
                REALM,
                MANAGEMENT_CLIENT_ID,
                MANAGEMENT_CLIENT_SECRET,
                IDENTITY_MANAGEMENT_CLIENT_ID,
                IDENTITY_MANAGEMENT_CLIENT_SECRET,
                CLIENT_ID,
                REDIRECT_URI,
                ADMIN_AUDIENCE,
                USERNAME,
                PASSWORD,
                OTP_SECRET);
    }
}
