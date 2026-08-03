package br.dev.andrestamatto.identityhub.integration.autoconfigure;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(
        classes = IdentityHubDefaultHttpSecurityTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class IdentityHubDefaultHttpSecurityTest {

    private static final TestJwtIssuer JWT_ISSUER = startJwtIssuer();

    @Autowired
    private MockMvc mvc;

    @AfterAll
    static void closeIssuer() {
        JWT_ISSUER.close();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("identityhub.security.issuer-uri", JWT_ISSUER::issuer);
        registry.add("identityhub.security.audience", () -> "catalog-api");
        registry.add("identityhub.security.allow-http-for-loopback", () -> true);
    }

    @Test
    void rejectsAnonymousAndWrongAudienceRequests() throws Exception {
        mvc.perform(get("/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("Bearer")));

        var now = Instant.now();
        var wrongAudienceToken = JWT_ISSUER.issueAccessToken(
                JWT_ISSUER.issuer(),
                List.of("another-api"),
                now,
                now.plusSeconds(300),
                "catalog:read",
                List.of());

        mvc.perform(get("/protected").header(HttpHeaders.AUTHORIZATION, "Bearer " + wrongAudienceToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsAValidBearerTokenForEveryDefaultProtectedRoute() throws Exception {
        var now = Instant.now();
        var token = JWT_ISSUER.issueAccessToken(
                JWT_ISSUER.issuer(),
                List.of("catalog-api"),
                now,
                now.plusSeconds(300),
                "catalog:read",
                List.of());

        mvc.perform(get("/protected").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("protected"));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(ProtectedEndpoint.class)
    static class TestApplication {
    }

    @RestController
    static class ProtectedEndpoint {

        @GetMapping("/protected")
        String protectedEndpoint() {
            return "protected";
        }
    }

    private static TestJwtIssuer startJwtIssuer() {
        try {
            return TestJwtIssuer.start();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not start the local JWT issuer", exception);
        }
    }
}
