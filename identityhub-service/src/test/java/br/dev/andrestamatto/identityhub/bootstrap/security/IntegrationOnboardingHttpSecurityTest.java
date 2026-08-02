package br.dev.andrestamatto.identityhub.bootstrap.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.dev.andrestamatto.identityhub.audit.application.AdministrativeAccessEventRepository;
import br.dev.andrestamatto.identityhub.clientapplication.adapter.out.jdbc.JdbcApplicationClientConfigurationRepository;
import br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationRepository;
import br.dev.andrestamatto.identityhub.identity.application.BeginOnboardingSession;
import br.dev.andrestamatto.identityhub.identity.application.OnboardingSessionRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude="
            + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
            + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
    "management.endpoint.health.group.readiness.include=readinessState,keycloak",
    "identityhub.security.admin.issuer-uri=https://auth.dev.example/realms/identityhub",
    "identityhub.security.admin.jwk-set-uri=https://auth.dev.example/realms/identityhub/certs",
    "identityhub.security.admin.audience=identityhub-admin-api",
    "identityhub.security.integration.audience=identityhub-integration-api",
    "identityhub.onboarding.enabled=true"
})
@AutoConfigureMockMvc
class IntegrationOnboardingHttpSecurityTest {

    private static final UUID MACHINE_CLIENT_ID =
            UUID.fromString("72c43df3-9f34-4dc6-85cc-5d323762f299");
    private static final String ENDPOINT = "/integration/v1/onboarding-sessions";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private AdministrativeAccessEventRepository auditRepository;

    @MockitoBean
    private ClientApplicationRepository clientApplicationRepository;

    @MockitoBean
    private JdbcApplicationClientConfigurationRepository clientRepository;

    @MockitoBean
    private br.dev.andrestamatto.identityhub.communication.application.EmailDeliveryRepository
            emailDeliveryRepository;

    @MockitoBean
    private OnboardingSessionRepository onboardingSessionRepository;

    @MockitoBean
    private BeginOnboardingSession beginOnboardingSession;

    @BeforeEach
    void successfulCreation() {
        when(beginOnboardingSession.execute(any())).thenReturn(
                new BeginOnboardingSession.Result(
                        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                        Instant.parse("2026-08-01T20:10:00Z"),
                        true));
    }

    @Test
    void requiresMachineAuthenticationAndOnboardingScope() throws Exception {
        mvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isUnauthorized());

        mvc.perform(post(ENDPOINT)
                        .with(jwt().jwt(token -> token.subject(MACHINE_CLIENT_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "purchase-2026-0001")
                        .content(body()))
                .andExpect(status().isForbidden());
    }

    @Test
    void createsAndReplaysSessionWithoutAcceptingApplicationAuthority() throws Exception {
        mvc.perform(post(ENDPOINT)
                        .with(machineJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "purchase-2026-0001")
                        .content(body()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.onboardingSession")
                        .value("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"))
                .andExpect(jsonPath("$.expiresAt").value("2026-08-01T20:10:00Z"))
                .andExpect(jsonPath("$.applicationId").doesNotExist())
                .andExpect(jsonPath("$.acquisitionReference").doesNotExist());

        when(beginOnboardingSession.execute(any())).thenReturn(
                new BeginOnboardingSession.Result(
                        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                        Instant.parse("2026-08-01T20:10:00Z"),
                        false));
        mvc.perform(post(ENDPOINT)
                        .with(machineJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "purchase-2026-0001")
                        .content(body()))
                .andExpect(status().isOk());

        mvc.perform(post(ENDPOINT)
                        .with(machineJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "purchase-2026-0001")
                        .content(body().replace(
                                "{", "{\"applicationId\":\"" + UUID.randomUUID() + "\",")))
                .andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor machineJwt() {
        return jwt()
                .jwt(token -> token.subject(MACHINE_CLIENT_ID.toString()))
                .authorities(new SimpleGrantedAuthority("SCOPE_onboarding:write"));
    }

    private String body() {
        return """
                {
                  "browserClientId": "4fef31b8-17db-40d8-af99-e2899b7db57c",
                  "acquisitionReference": "order-2026-0001",
                  "redirectUri": "https://app.example.com/auth/callback",
                  "codeChallenge": "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"
                }
                """;
    }
}
