package br.dev.andrestamatto.identityhub.identity.adapter.in.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.dev.andrestamatto.identityhub.audit.application.AdministrativeAccessEventRepository;
import br.dev.andrestamatto.identityhub.bootstrap.IdentityHubApplication;
import br.dev.andrestamatto.identityhub.identity.adapter.out.jdbc.JdbcGlobalAccountDisableOperationRepository;
import br.dev.andrestamatto.identityhub.identity.application.DisableGlobalAccount;
import br.dev.andrestamatto.identityhub.identity.application.GlobalAccountDisableOperation;
import br.dev.andrestamatto.identityhub.identity.application.GlobalAccountDisableStatus;
import br.dev.andrestamatto.identityhub.identity.domain.UserAccountRef;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionOperations;

@SpringBootTest(classes = IdentityHubApplication.class, properties = {
    "spring.autoconfigure.exclude="
            + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
            + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
    "management.endpoint.health.group.readiness.include=readinessState,keycloak",
    "identityhub.security.admin.issuer-uri=https://auth.dev.example/realms/identityhub",
    "identityhub.security.admin.jwk-set-uri=https://auth.dev.example/realms/identityhub/certs",
    "identityhub.security.admin.audience=identityhub-admin-api",
    "identityhub.keycloak.identity-management.enabled=true",
    "identityhub.keycloak.identity-management.base-uri=http://127.0.0.1:9999",
    "identityhub.keycloak.identity-management.realm=identityhub-test",
    "identityhub.keycloak.identity-management.client-id=identity-management",
    "identityhub.keycloak.identity-management.client-secret=test-only-management-secret",
    "identityhub.keycloak.identity-management.public-base-uri=http://127.0.0.1:8080"
})
@AutoConfigureMockMvc
class GlobalAccountAdminHttpTest {

    private static final Instant NOW = Instant.parse("2026-08-02T18:00:00Z");
    private static final UUID ACCOUNT_ID =
            UUID.fromString("f20ace9e-e02a-436f-93e6-edaff0320733");
    private static final String PATH =
            "/internal/admin/user-accounts/" + ACCOUNT_ID + "/disable";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private JdbcClient jdbcClient;

    @MockitoBean
    private TransactionOperations transactionOperations;

    @MockitoBean
    private AdministrativeAccessEventRepository auditRepository;

    @MockitoBean
    private JdbcGlobalAccountDisableOperationRepository operationRepository;

    @MockitoBean
    private DisableGlobalAccount disableGlobalAccount;

    @MockitoBean
    private Clock clock;

    @BeforeEach
    void configureClock() {
        when(clock.instant()).thenReturn(NOW);
    }

    @Test
    void recentPlatformAdministratorDisablesAccountThroughPrivateContract() throws Exception {
        when(disableGlobalAccount.execute(any())).thenReturn(completedOperation());

        mvc.perform(post(PATH)
                        .with(admin(NOW.minusSeconds(30)))
                        .header("Idempotency-Key", "disable-account-001")
                        .header("X-Correlation-ID", "disable-account-correlation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Confirmed security incident\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string(
                        "X-Correlation-ID", "disable-account-correlation"))
                .andExpect(jsonPath("$.operationId")
                        .value(completedOperation().operationId().toString()))
                .andExpect(jsonPath("$.userAccountRef").value(ACCOUNT_ID.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.actorSubject").doesNotExist())
                .andExpect(jsonPath("$.reason").doesNotExist());

        verify(disableGlobalAccount).execute(any(DisableGlobalAccount.Command.class));
    }

    @Test
    void deniesAuditorMissingTotpAndStaleAuthentication() throws Exception {
        mvc.perform(request(auditor(NOW.minusSeconds(30))))
                .andExpect(status().isForbidden());
        mvc.perform(request(adminWithoutTotp(NOW.minusSeconds(30))))
                .andExpect(status().isForbidden());
        mvc.perform(request(admin(NOW.minusSeconds(301))))
                .andExpect(status().isForbidden());

        verify(disableGlobalAccount, never()).execute(any());
    }

    @Test
    void requiresIdempotencyKeyAndRejectsUnknownJsonFields() throws Exception {
        mvc.perform(post(PATH)
                        .with(admin(NOW.minusSeconds(30)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Confirmed security incident\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(PATH)
                        .with(admin(NOW.minusSeconds(30)))
                        .header("Idempotency-Key", "disable-account-002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Confirmed security incident\","
                                + "\"unexpected\":true}"))
                .andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request(
            RequestPostProcessor authentication) {
        return post(PATH)
                .with(authentication)
                .header("Idempotency-Key", "disable-account-denied")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Confirmed security incident\"}");
    }

    private RequestPostProcessor admin(Instant authenticationTime) {
        return jwt().jwt(token -> token
                        .subject("admin-subject")
                        .claim("auth_time", authenticationTime))
                .authorities(
                        new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"),
                        new SimpleGrantedAuthority("MFA_TOTP"));
    }

    private RequestPostProcessor adminWithoutTotp(Instant authenticationTime) {
        return jwt().jwt(token -> token
                        .subject("admin-subject")
                        .claim("auth_time", authenticationTime))
                .authorities(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"));
    }

    private RequestPostProcessor auditor(Instant authenticationTime) {
        return jwt().jwt(token -> token
                        .subject("auditor-subject")
                        .claim("auth_time", authenticationTime))
                .authorities(
                        new SimpleGrantedAuthority("ROLE_PLATFORM_AUDITOR"),
                        new SimpleGrantedAuthority("MFA_TOTP"));
    }

    private GlobalAccountDisableOperation completedOperation() {
        return new GlobalAccountDisableOperation(
                UUID.fromString("0658c077-6544-47fc-9755-d7491f07dc5b"),
                new UserAccountRef(ACCOUNT_ID),
                "Confirmed security incident",
                "disable-account-001",
                "b9b8c6601d9ce559b9b8c6601d9ce559b9b8c6601d9ce559b9b8c6601d9ce559",
                "admin-subject",
                "disable-account-correlation",
                GlobalAccountDisableStatus.COMPLETED,
                null,
                NOW.minusSeconds(1),
                NOW);
    }
}
