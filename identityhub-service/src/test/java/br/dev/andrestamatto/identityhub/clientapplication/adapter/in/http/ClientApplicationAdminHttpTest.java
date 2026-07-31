package br.dev.andrestamatto.identityhub.clientapplication.adapter.in.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.dev.andrestamatto.identityhub.audit.application.AdministrativeAccessEventRepository;
import br.dev.andrestamatto.identityhub.audit.application.AdministrativeAccessOutcome;
import br.dev.andrestamatto.identityhub.bootstrap.IdentityHubApplication;
import br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationRepository;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationIdentifier;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplication;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplicationId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.DisplayName;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
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
class ClientApplicationAdminHttpTest {

    private static final UUID APPLICATION_ID =
            UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0");
    private static final String PATH = "/internal/admin/client-applications/" + APPLICATION_ID;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private AdministrativeAccessEventRepository auditRepository;

    @MockitoBean
    private ClientApplicationRepository repository;

    @Test
    void adminWithTotpRegistersDraftApplication() throws Exception {
        when(repository.findById(any())).thenReturn(Optional.empty());
        when(repository.findByIdentifier(any())).thenReturn(Optional.empty());

        mvc.perform(put(PATH)
                        .with(adminWithTotp())
                        .header("X-Correlation-ID", "register-auto-radar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("auto-radar", "Auto Radar")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", PATH))
                .andExpect(header().string("X-Correlation-ID", "register-auto-radar"))
                .andExpect(jsonPath("$.applicationId").value(APPLICATION_ID.toString()))
                .andExpect(jsonPath("$.identifier").value("auto-radar"))
                .andExpect(jsonPath("$.displayName").value("Auto Radar"))
                .andExpect(jsonPath("$.state").value("DRAFT"));

        verify(repository).add(any(ClientApplication.class));
        verify(auditRepository).append(argThat(event ->
                event.correlationId().equals("register-auto-radar")
                        && event.method().equals("PUT")
                        && event.path().equals(PATH)
                        && event.outcome() == AdministrativeAccessOutcome.ALLOWED));
    }

    @Test
    void identicalRetryReturnsCurrentApplicationWithoutAdding() throws Exception {
        when(repository.findById(any())).thenReturn(Optional.of(application()));

        mvc.perform(put(PATH)
                        .with(adminWithTotp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("auto-radar", "Auto Radar")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registeredAt").value("2026-07-30T14:00:00Z"));

        verify(repository, never()).add(any());
    }

    @Test
    void auditorWithTotpCanReadApplication() throws Exception {
        when(repository.findById(any())).thenReturn(Optional.of(application()));

        mvc.perform(get(PATH).with(auditorWithTotp()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifier").value("auto-radar"));
    }

    @Test
    void auditorCannotRegisterApplication() throws Exception {
        mvc.perform(put(PATH)
                        .with(auditorWithTotp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("auto-radar", "Auto Radar")))
                .andExpect(status().isForbidden());

        verify(repository, never()).add(any());
        verify(auditRepository).append(argThat(event ->
                event.method().equals("PUT")
                        && event.path().equals(PATH)
                        && event.outcome() == AdministrativeAccessOutcome.DENIED));
    }

    @Test
    void invalidApplicationReturnsSafeBadRequest() throws Exception {
        mvc.perform(put(PATH)
                        .with(adminWithTotp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("INVALID", "Auto Radar")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid client application"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void duplicateIdentifierReturnsSafeConflict() throws Exception {
        when(repository.findById(any())).thenReturn(Optional.empty());
        when(repository.findByIdentifier(any())).thenReturn(Optional.of(application()));

        mvc.perform(put(PATH)
                        .with(adminWithTotp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("auto-radar", "Auto Radar")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Client application conflict"))
                .andExpect(jsonPath("$.status").value(409));

        verify(repository, never()).add(any());
    }

    @Test
    void unknownApplicationReturnsNotFound() throws Exception {
        when(repository.findById(any())).thenReturn(Optional.empty());

        mvc.perform(get(PATH).with(auditorWithTotp()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Client application not found"));
    }

    private RequestPostProcessor adminWithTotp() {
        return jwt().authorities(
                new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"),
                new SimpleGrantedAuthority("MFA_TOTP"));
    }

    private RequestPostProcessor auditorWithTotp() {
        return jwt().authorities(
                new SimpleGrantedAuthority("ROLE_PLATFORM_AUDITOR"),
                new SimpleGrantedAuthority("MFA_TOTP"));
    }

    private String requestBody(String identifier, String displayName) {
        return """
                {
                  "identifier": "%s",
                  "displayName": "%s"
                }
                """.formatted(identifier, displayName);
    }

    private ClientApplication application() {
        return ClientApplication.register(
                new ClientApplicationId(APPLICATION_ID),
                new ApplicationIdentifier("auto-radar"),
                new DisplayName("Auto Radar"),
                Clock.fixed(Instant.parse("2026-07-30T14:00:00Z"), ZoneOffset.UTC));
    }
}
