package br.dev.andrestamatto.identityhub.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidationException;
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

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KeycloakAdminTokenIntegrationTest {

    private static final String REALM = "identityhub-test";
    private static final String CLIENT_ID = "identityhub-admin-login";
    private static final String ADMIN_AUDIENCE = "identityhub-admin-api";
    private static final String USERNAME = "platform-admin";
    private static final String PASSWORD = syntheticPassword();
    private static final String OTP_SECRET = String.join("", "JBSWY3DP", "EHPK3PXP");
    private static final String REDIRECT_URI = "http://127.0.0.1/callback";
    private static final String PKCE_VERIFIER =
            "test-only-verifier-identityhub-admin-login-0001";
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
    }

    @BeforeAll
    static void configureRealm() throws Exception {
        keycloakBaseUri = keycloakBaseUri();
        var adminToken = requestBootstrapAdminToken();
        createRealm(adminToken);
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
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(MessageDigest.getInstance("SHA-256")
                        .digest(PKCE_VERIFIER.getBytes(StandardCharsets.US_ASCII)));
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
                CLIENT_ID,
                REDIRECT_URI,
                ADMIN_AUDIENCE,
                USERNAME,
                PASSWORD,
                OTP_SECRET);
    }
}
