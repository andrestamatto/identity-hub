package br.dev.andrestamatto.identityhub.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.autoconfigure.exclude="
                    + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                    + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
            "management.endpoint.health.group.readiness.include=readinessState",
            "identityhub.security.admin.issuer-uri=https://auth.dev.example/realms/identityhub",
            "identityhub.security.admin.jwk-set-uri=https://auth.dev.example/realms/identityhub/certs",
            "identityhub.security.admin.audience=identityhub-admin-api"
        })
class FoundationHttpTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private br.dev.andrestamatto.identityhub.audit.application.AdministrativeAccessEventRepository
            administrativeAccessEventRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationRepository
            clientApplicationRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private br.dev.andrestamatto.identityhub.clientapplication.adapter.out.jdbc
                    .JdbcApplicationClientConfigurationRepository
            applicationClientConfigurationRepository;

    @Test
    void exposesMinimalHealthWithoutDetails() throws Exception {
        var liveness = get("/actuator/health/liveness");
        var readiness = get("/actuator/health/readiness");

        assertThat(liveness.statusCode()).isEqualTo(200);
        assertThat(liveness.body()).isEqualTo("{\"status\":\"UP\"}");
        assertThat(readiness.statusCode()).isEqualTo(200);
        assertThat(readiness.body()).isEqualTo("{\"status\":\"UP\"}");
    }

    @Test
    void deniesLegacyFunctionalRoute() throws Exception {
        var response = get("/users/register");

        assertThat(response.statusCode()).isEqualTo(401);
    }

    private HttpResponse<String> get(String path) throws Exception {
        var request = HttpRequest.newBuilder(uri(path)).GET().build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }
}
