package br.dev.andrestamatto.identityhub.bootstrap.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.dev.andrestamatto.identityhub.audit.application.AdministrativeAccessEventRepository;
import br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationRepository;
import br.dev.andrestamatto.identityhub.clientapplication.adapter.out.jdbc
        .JdbcApplicationClientConfigurationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude="
            + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
            + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
    "management.endpoint.health.group.readiness.include=readinessState,keycloak",
    "identityhub.security.admin.issuer-uri=https://auth.dev.example/realms/identityhub",
    "identityhub.security.admin.jwk-set-uri=https://auth.dev.example/realms/identityhub/certs",
    "identityhub.security.admin.audience=identityhub-admin-api"
})
@AutoConfigureMockMvc
class AdminHttpSecurityTest {

    private static final String BFF_SECRET_ENDPOINT = "/internal/admin/client-applications/"
            + "184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0/clients/"
            + "72c43df3-9f34-4dc6-85cc-5d323762f299/credentials/client-secret";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private AdministrativeAccessEventRepository administrativeAccessEventRepository;

    @MockitoBean
    private ClientApplicationRepository clientApplicationRepository;

    @MockitoBean
    private JdbcApplicationClientConfigurationRepository applicationClientConfigurationRepository;

    @Test
    void requiresAuthenticationForAdministrativeEndpoints() throws Exception {
        mvc.perform(get("/internal/admin/runtime"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsAuditorWithTotpToReadRuntimeInformation() throws Exception {
        mvc.perform(get("/internal/admin/runtime")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_PLATFORM_AUDITOR"),
                                new SimpleGrantedAuthority("MFA_TOTP"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.environment").value("DEVELOPMENT"))
                .andExpect(jsonPath("$.issuer").value("https://auth.dev.example/realms/identityhub"));
    }

    @Test
    void rejectsAdministrativeRoleWithoutTotpEvidence() throws Exception {
        mvc.perform(get("/internal/admin/runtime")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditorCannotReachAdministrativeMutation() throws Exception {
        mvc.perform(put("/internal/admin/client-applications/"
                        + "184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_PLATFORM_AUDITOR"),
                                new SimpleGrantedAuthority("MFA_TOTP")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier": "auto-radar",
                                  "displayName": "Auto Radar"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectsBffSecretRotationWithAuthenticationAndAdminTotp() throws Exception {
        mvc.perform(post(BFF_SECRET_ENDPOINT))
                .andExpect(status().isUnauthorized());

        mvc.perform(post(BFF_SECRET_ENDPOINT)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"))))
                .andExpect(status().isForbidden());

        mvc.perform(post(BFF_SECRET_ENDPOINT)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_PLATFORM_AUDITOR"),
                                new SimpleGrantedAuthority("MFA_TOTP"))))
                .andExpect(status().isForbidden());
    }
}
