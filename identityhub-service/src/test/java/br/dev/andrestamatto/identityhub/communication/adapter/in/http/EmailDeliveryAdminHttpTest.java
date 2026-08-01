package br.dev.andrestamatto.identityhub.communication.adapter.in.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.dev.andrestamatto.identityhub.audit.application.AdministrativeAccessEventRepository;
import br.dev.andrestamatto.identityhub.bootstrap.IdentityHubApplication;
import br.dev.andrestamatto.identityhub.clientapplication.adapter.out.jdbc.JdbcApplicationClientConfigurationRepository;
import br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationRepository;
import br.dev.andrestamatto.identityhub.communication.application.EmailDelivery;
import br.dev.andrestamatto.identityhub.communication.application.EmailDeliveryPurpose;
import br.dev.andrestamatto.identityhub.communication.application.EmailDeliveryRepository;
import br.dev.andrestamatto.identityhub.communication.application.EmailDeliveryState;
import br.dev.andrestamatto.identityhub.communication.domain.EmailDeliveryId;
import br.dev.andrestamatto.identityhub.communication.domain.EmailRecipient;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
class EmailDeliveryAdminHttpTest {

    private static final UUID DELIVERY_ID =
            UUID.fromString("27f3aa0b-6a70-43bd-a087-d5bc0c1bc779");
    private static final String PATH =
            "/internal/admin/communication/email-deliveries/" + DELIVERY_ID;

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
    private EmailDeliveryRepository emailDeliveryRepository;

    @Test
    void auditorReadsSanitizedDeliveryDiagnostics() throws Exception {
        when(emailDeliveryRepository.find(any())).thenReturn(Optional.of(delivery()));

        mvc.perform(get(PATH).with(auditorWithTotp()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryId").value(DELIVERY_ID.toString()))
                .andExpect(jsonPath("$.applicationIdentifier").value("auto-radar"))
                .andExpect(jsonPath("$.state").value("FAILED"))
                .andExpect(jsonPath("$.lastFailureCode").value("INVALID_MESSAGE"))
                .andExpect(jsonPath("$.recipient").doesNotExist())
                .andExpect(jsonPath("$.correlationId").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("andre@example.com"))));
    }

    @Test
    void adminCanReprocessTerminalFailure() throws Exception {
        when(emailDeliveryRepository.requeue(any(), any()))
                .thenReturn(Optional.of(pendingDelivery()));

        mvc.perform(post(PATH + "/reprocess").with(adminWithTotp()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.state").value("PENDING"));
    }

    @Test
    void auditorCannotReprocessDelivery() throws Exception {
        mvc.perform(post(PATH + "/reprocess").with(auditorWithTotp()))
                .andExpect(status().isForbidden());

        verify(emailDeliveryRepository, never()).requeue(any(), any());
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

    private EmailDelivery delivery() {
        return EmailDelivery.reconstitute(
                new EmailDeliveryId(DELIVERY_ID),
                UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0"),
                "auto-radar",
                "Auto Radar",
                "development",
                new EmailRecipient("andre@example.com"),
                EmailDeliveryPurpose.PASSWORD_CHANGED,
                EmailDeliveryState.FAILED,
                3,
                Instant.parse("2026-07-31T18:00:00Z"),
                "INVALID_MESSAGE",
                "secret-correlation",
                Instant.parse("2026-07-31T17:00:00Z"),
                Instant.parse("2026-07-31T18:00:00Z"));
    }

    private EmailDelivery pendingDelivery() {
        var failed = delivery();
        return EmailDelivery.reconstitute(
                failed.id(), failed.applicationId(), failed.applicationIdentifier(),
                failed.applicationDisplayName(), failed.environment(), failed.recipient(),
                failed.purpose(), EmailDeliveryState.PENDING, 0, failed.nextAttemptAt(),
                null, failed.correlationId(), failed.requestedAt(), failed.updatedAt());
    }
}
