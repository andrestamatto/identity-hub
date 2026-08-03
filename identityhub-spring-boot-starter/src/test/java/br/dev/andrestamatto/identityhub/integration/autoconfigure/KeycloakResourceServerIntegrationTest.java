package br.dev.andrestamatto.identityhub.integration.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@Testcontainers(disabledWithoutDocker = true)
class KeycloakResourceServerIntegrationTest {

    private static final String REALM = "identityhub-starter-test";
    private static final String AUDIENCE = "catalog-api";
    private static final String MACHINE_CLIENT_ID = "catalog-machine";
    private static final String MACHINE_CLIENT_SECRET = "starter-test-machine-secret";
    private static final Pattern ACCESS_TOKEN = Pattern.compile("\"access_token\":\"([^\"]+)\"");
    private static final Pattern OIDC_ERROR = Pattern.compile("\"error\":\"([a-z_]+)\"");

    @Container
    private static final GenericContainer<?> KEYCLOAK = new GenericContainer<>(
            DockerImageName.parse("quay.io/keycloak/keycloak:26.7.0"))
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource("keycloak/identityhub-starter-test-realm.json"),
                            "/opt/keycloak/data/import/identityhub-starter-test-realm.json")
                    .withCommand("start-dev", "--import-realm", "--http-enabled=true", "--hostname-strict=false")
                    .withExposedPorts(8080)
                    .waitingFor(Wait.forHttp("/realms/" + REALM + "/.well-known/openid-configuration")
                            .forStatusCode(200)
                            .withStartupTimeout(Duration.ofMinutes(4)));

    @Test
    void validatesARealKeycloakAccessTokenThroughDiscoveryAndJwks() throws Exception {
        var issuer = issuer();
        var properties = new IdentityHubSecurityProperties(
                true,
                issuer,
                AUDIENCE,
                Duration.ofSeconds(60),
                true,
                null);
        var configuration = new IdentityHubSecurityAutoConfiguration();
        OAuth2TokenValidator<Jwt> validator = configuration.identityHubJwtValidator(
                properties,
                Clock.systemUTC());
        JwtDecoder decoder = configuration.identityHubJwtDecoder(
                properties,
                validator,
                configuration.identityHubJwtRestOperations());

        var jwt = decoder.decode(requestAccessToken());
        var authentication = new IdentityHubJwtAuthenticationConverter(properties.authorities()).convert(jwt);

        assertThat(jwt.getIssuer().toString()).isEqualTo(issuer.toString());
        assertThat(jwt.getAudience()).contains(AUDIENCE);
        assertThat(jwt.getClaimAsString("scope")).isNotBlank();
        assertThat(jwt.getClaimAsStringList("roles")).isEmpty();
        assertThat(authentication.getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .doesNotContain("ROLE_PLATFORM_ADMIN");
    }

    private URI issuer() {
        return URI.create("http://" + KEYCLOAK.getHost() + ":" + KEYCLOAK.getMappedPort(8080)
                + "/realms/" + REALM);
    }

    private String requestAccessToken() throws Exception {
        var response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(issuer() + "/protocol/openid-connect/token"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(
                        "grant_type=client_credentials&client_id=" + MACHINE_CLIENT_ID
                                        + "&client_secret=" + MACHINE_CLIENT_SECRET))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode())
                .withFailMessage(
                        "Keycloak token endpoint returned %s (%s)",
                        response.statusCode(),
                        oidcError(response.body()))
                .isEqualTo(200);
        Matcher matcher = ACCESS_TOKEN.matcher(response.body());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String oidcError(String body) {
        Matcher matcher = OIDC_ERROR.matcher(body);
        return matcher.find() ? matcher.group(1) : "unknown";
    }
}
