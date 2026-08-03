package br.dev.andrestamatto.identityhub.access.adapter.in.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.dev.andrestamatto.identityhub.access.application.GrantMembership;
import br.dev.andrestamatto.identityhub.access.application.GetMembershipOperation;
import br.dev.andrestamatto.identityhub.access.application.MembershipGrantConflictException;
import br.dev.andrestamatto.identityhub.access.application.MembershipGrantRepository;
import br.dev.andrestamatto.identityhub.access.application.MembershipGrantResult;
import br.dev.andrestamatto.identityhub.access.application.MembershipOperationStatus;
import br.dev.andrestamatto.identityhub.audit.application.AdministrativeAccessEventRepository;
import br.dev.andrestamatto.identityhub.bootstrap.IdentityHubApplication;
import br.dev.andrestamatto.identityhub.clientapplication.adapter.out.jdbc
        .JdbcApplicationClientConfigurationRepository;
import br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationRepository;
import br.dev.andrestamatto.identityhub.clientapplication.application.MembershipProvisioningClient;
import br.dev.andrestamatto.identityhub.clientapplication.application
        .MembershipProvisioningClientResolver;
import java.time.Instant;
import java.util.Optional;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(classes = IdentityHubApplication.class, properties = {
    "spring.autoconfigure.exclude="
            + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
            + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
    "management.endpoint.health.group.readiness.include=readinessState,keycloak",
    "identityhub.security.admin.issuer-uri=https://auth.dev.example/realms/identityhub",
    "identityhub.security.admin.jwk-set-uri=https://auth.dev.example/realms/identityhub/certs",
    "identityhub.security.admin.audience=identityhub-admin-api"
})
@AutoConfigureMockMvc
class MembershipGrantHttpTest {

    private static final UUID APPLICATION_ID =
            UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0");
    private static final UUID APPLICATION_CLIENT_ID =
            UUID.fromString("72c43df3-9f34-4dc6-85cc-5d323762f299");
    private static final UUID USER_ACCOUNT_REF =
            UUID.fromString("680ac2e4-bfb0-4375-a75e-453b6e7b600c");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private GrantMembership grantMembership;

    @MockitoBean
    private GetMembershipOperation getMembershipOperation;

    @MockitoBean
    private MembershipGrantRepository membershipRepository;

    @MockitoBean
    private MembershipProvisioningClientResolver provisioningClientResolver;

    @MockitoBean
    private AdministrativeAccessEventRepository auditRepository;

    @MockitoBean
    private ClientApplicationRepository clientApplicationRepository;

    @MockitoBean
    private JdbcApplicationClientConfigurationRepository clientConfigurationRepository;

    @MockitoBean
    private br.dev.andrestamatto.identityhub.communication.application.EmailDeliveryRepository
            emailDeliveryRepository;

    @BeforeEach
    void authorizeProvisioner() {
        when(provisioningClientResolver.resolve("ih-machine-" + APPLICATION_CLIENT_ID))
                .thenReturn(Optional.of(new MembershipProvisioningClient(
                        APPLICATION_ID, APPLICATION_CLIENT_ID)));
        when(grantMembership.execute(any())).thenReturn(new MembershipGrantResult(
                UUID.fromString("06d068e0-78d5-4df6-97dd-88df538ab948"),
                UUID.fromString("c50638fe-0b91-4f47-81e6-2bd183040c1c"),
                APPLICATION_ID,
                USER_ACCOUNT_REF,
                "PENDING",
                Instant.parse("2026-08-02T18:00:00Z")));
    }

    @Test
    void authorizedMachineClientCreatesPendingMembershipIntent() throws Exception {
        mvc.perform(post("/api/v1/memberships")
                        .with(jwt()
                                .jwt(token -> token.claim(
                                        "azp", "ih-machine-" + APPLICATION_CLIENT_ID))
                                .authorities(new SimpleGrantedAuthority(
                                        "SCOPE_membership:write")))
                        .header("Idempotency-Key", "membership-grant-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userAccountRef": "%s"
                                }
                                """.formatted(USER_ACCOUNT_REF)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.operationId").isNotEmpty())
                .andExpect(jsonPath("$.membershipId").isNotEmpty())
                .andExpect(jsonPath("$.state").value("PENDING"))
                .andExpect(jsonPath("$.applicationId").doesNotExist());
    }

    @Test
    void requiresAuthenticationAndMembershipWriteScope() throws Exception {
        mvc.perform(post("/api/v1/memberships")
                        .header("Idempotency-Key", "membership-grant-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userAccountRef\":\"" + USER_ACCOUNT_REF + "\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/memberships")
                        .with(jwt().jwt(token -> token.claim(
                                "azp", "ih-machine-" + APPLICATION_CLIENT_ID)))
                        .header("Idempotency-Key", "membership-grant-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userAccountRef\":\"" + USER_ACCOUNT_REF + "\"}"))
                .andExpect(status().isForbidden());

        verify(grantMembership, never()).execute(any());
    }

    @Test
    void rejectsAzpThatDoesNotResolveToAnAuthorizedClient() throws Exception {
        when(provisioningClientResolver.resolve("unknown-client"))
                .thenReturn(Optional.empty());

        mvc.perform(post("/api/v1/memberships")
                        .with(jwt()
                                .jwt(token -> token.claim("azp", "unknown-client"))
                                .authorities(new SimpleGrantedAuthority(
                                        "SCOPE_membership:write")))
                        .header("Idempotency-Key", "membership-grant-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userAccountRef\":\"" + USER_ACCOUNT_REF + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Membership provisioning denied"));

        verify(grantMembership, never()).execute(any());
    }

    @Test
    void rejectsApplicationIdentifierAndUnknownPayloadFields() throws Exception {
        mvc.perform(post("/api/v1/memberships")
                        .with(jwt()
                                .jwt(token -> token.claim(
                                        "azp", "ih-machine-" + APPLICATION_CLIENT_ID))
                                .authorities(new SimpleGrantedAuthority(
                                        "SCOPE_membership:write")))
                        .header("Idempotency-Key", "membership-grant-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userAccountRef": "%s",
                                  "applicationId": "%s"
                                }
                                """.formatted(USER_ACCOUNT_REF, UUID.randomUUID())))
                .andExpect(status().isBadRequest());

        verify(grantMembership, never()).execute(any());
    }

    @Test
    void reportsIdempotencyConflictWithoutChangingAuthorization() throws Exception {
        when(grantMembership.execute(any())).thenThrow(new MembershipGrantConflictException());

        mvc.perform(post("/api/v1/memberships")
                        .with(jwt()
                                .jwt(token -> token.claim(
                                        "azp", "ih-machine-" + APPLICATION_CLIENT_ID))
                                .authorities(new SimpleGrantedAuthority(
                                        "SCOPE_membership:write")))
                        .header("Idempotency-Key", "membership-grant-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userAccountRef\":\"" + USER_ACCOUNT_REF + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Idempotency conflict"));
    }

    @Test
    void directErrorEndpointRequestRemainsDenied() throws Exception {
        mvc.perform(get("/error")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                "SCOPE_membership:write"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void authorizedClientReadsOnlyItsOperationStatus() throws Exception {
        var operationId = UUID.fromString("06d068e0-78d5-4df6-97dd-88df538ab948");
        when(getMembershipOperation.execute(operationId, APPLICATION_ID))
                .thenReturn(Optional.of(new MembershipOperationStatus(
                        operationId,
                        UUID.fromString("c50638fe-0b91-4f47-81e6-2bd183040c1c"),
                        "ACTIVE",
                        "APPLIED",
                        1,
                        null,
                        Instant.parse("2026-08-02T18:00:00Z"),
                        Instant.parse("2026-08-02T18:00:01Z"))));

        mvc.perform(get("/api/v1/membership-operations/" + operationId)
                        .with(provisionerJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.membershipState").value("ACTIVE"))
                .andExpect(jsonPath("$.projectionState").value("APPLIED"))
                .andExpect(jsonPath("$.applicationId").doesNotExist())
                .andExpect(jsonPath("$.userAccountRef").doesNotExist());
    }

    @Test
    void crossApplicationOperationIsIndistinguishableFromUnknown() throws Exception {
        var operationId = UUID.fromString("06d068e0-78d5-4df6-97dd-88df538ab948");
        when(getMembershipOperation.execute(operationId, APPLICATION_ID))
                .thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/membership-operations/" + operationId)
                        .with(provisionerJwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Membership operation not found"));
    }

    private RequestPostProcessor provisionerJwt() {
        return jwt()
                .jwt(token -> token.claim("azp", "ih-machine-" + APPLICATION_CLIENT_ID))
                .authorities(new SimpleGrantedAuthority("SCOPE_membership:write"));
    }

}
