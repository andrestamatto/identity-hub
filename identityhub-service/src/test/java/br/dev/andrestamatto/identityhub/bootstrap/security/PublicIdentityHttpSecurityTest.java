package br.dev.andrestamatto.identityhub.bootstrap.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import br.dev.andrestamatto.identityhub.audit.application.AdministrativeAccessEventRepository;
import br.dev.andrestamatto.identityhub.clientapplication.adapter.out.jdbc
        .JdbcApplicationClientConfigurationRepository;
import br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationRepository;
import br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationSnapshot;
import br.dev.andrestamatto.identityhub.clientapplication.application.GetClientApplicationByIdentifier;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplicationState;
import br.dev.andrestamatto.identityhub.clientapplication.domain.SelfRegistrationPolicy;
import br.dev.andrestamatto.identityhub.identity.adapter.in.http
        .InMemoryRegistrationRateLimiter;
import br.dev.andrestamatto.identityhub.identity.adapter.in.http
        .InMemoryPasswordRecoveryRateLimiter;
import br.dev.andrestamatto.identityhub.identity.adapter.in.http.PublicResponseTiming;
import br.dev.andrestamatto.identityhub.identity.application.BeginLocalRegistration;
import br.dev.andrestamatto.identityhub.identity.application.ConfirmEmailVerification;
import br.dev.andrestamatto.identityhub.identity.application.CompletePasswordRecovery;
import br.dev.andrestamatto.identityhub.identity.application.RequestPasswordRecovery;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude="
            + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
            + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
    "identityhub.public-identity.enabled=true",
    "identityhub.keycloak.identity-management.enabled=false",
    "management.endpoint.health.group.readiness.include=readinessState,keycloak",
    "identityhub.security.admin.issuer-uri=https://auth.dev.example/realms/identityhub",
    "identityhub.security.admin.jwk-set-uri=https://auth.dev.example/realms/identityhub/certs",
    "identityhub.security.admin.audience=identityhub-admin-api"
})
@AutoConfigureMockMvc
class PublicIdentityHttpSecurityTest {

    private static final String REGISTRATION_ENDPOINT =
            "/public/v1/applications/auto-radar/local-registrations";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private AdministrativeAccessEventRepository administrativeAccessEventRepository;

    @MockitoBean
    private ClientApplicationRepository clientApplicationRepository;

    @MockitoBean
    private JdbcApplicationClientConfigurationRepository applicationClientRepository;

    @MockitoBean
    private br.dev.andrestamatto.identityhub.communication.application.EmailDeliveryRepository
            emailDeliveryRepository;

    @MockitoBean
    private br.dev.andrestamatto.identityhub.access.application.MembershipGrantRepository
            membershipGrantRepository;

    @MockitoBean
    private GetClientApplicationByIdentifier getApplication;

    @MockitoBean
    private BeginLocalRegistration beginRegistration;

    @MockitoBean
    private ConfirmEmailVerification confirmVerification;

    @MockitoBean
    private RequestPasswordRecovery requestPasswordRecovery;

    @MockitoBean
    private CompletePasswordRecovery completePasswordRecovery;

    @MockitoBean
    private InMemoryRegistrationRateLimiter rateLimiter;

    @MockitoBean
    private InMemoryPasswordRecoveryRateLimiter recoveryRateLimiter;

    @MockitoBean
    private PublicResponseTiming responseTiming;

    @BeforeEach
    void setUp() {
        when(getApplication.execute("auto-radar")).thenReturn(new ClientApplicationSnapshot(
                UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0"),
                "auto-radar",
                "Auto Radar",
                ClientApplicationState.DRAFT,
                SelfRegistrationPolicy.ENABLED,
                Instant.parse("2026-08-01T10:00:00Z")));
        when(beginRegistration.execute(any())).thenReturn(new BeginLocalRegistration.Result(
                UUID.fromString("fbd31357-31b8-46dc-9ec7-38b0c72d1207"),
                UUID.fromString("1e04b771-df2f-45e1-bc45-f22d46da11b5")));
    }

    @Test
    void permitsOnlyTheExplicitPublicPostEndpointsWithoutAuthentication() throws Exception {
        mvc.perform(post(REGISTRATION_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "andre@example.com",
                                  "password": "correct-horse-battery"
                                }
                                """))
                .andExpect(status().isAccepted());

        mvc.perform(post("/public/v1/email-verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"challenge.secret\"}"))
                .andExpect(status().isNoContent());

        mvc.perform(post("/public/v1/applications/auto-radar/password-recoveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"andre@example.com\"}"))
                .andExpect(status().isAccepted());

        mvc.perform(post("/public/v1/password-recoveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"challenge.secret\","
                                + "\"newPassword\":\"a new secure password phrase\"}"))
                .andExpect(status().isNoContent());

        mvc.perform(get(REGISTRATION_ENDPOINT))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/public/v1/password-recoveries"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/public/v1/unrecognized"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsUnknownAndDuplicateFieldsWithoutLeakingParserDetails() throws Exception {
        mvc.perform(post(REGISTRATION_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "andre@example.com",
                                  "password": "correct-horse-battery",
                                  "unexpected": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "The request body does not match the expected schema"));

        mvc.perform(post(REGISTRATION_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "andre@example.com",
                                  "email": "attacker@example.com",
                                  "password": "correct-horse-battery"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "The request body does not match the expected schema"));
    }

    @Test
    void rejectsOversizedBodiesBeforeJsonDeserialization() throws Exception {
        mvc.perform(post(REGISTRATION_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(" ".repeat(2_049)))
                .andExpect(status().is(413))
                .andExpect(jsonPath("$.detail").value(
                        "The request body exceeds the allowed size"));
    }
}
