package br.dev.andrestamatto.identityhub.identity.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.dev.andrestamatto.identityhub.identity.application.DisableGlobalAccount;
import br.dev.andrestamatto.identityhub.identity.application.GlobalAccountDisableOperation;
import br.dev.andrestamatto.identityhub.identity.application.GlobalAccountDisableStatus;
import br.dev.andrestamatto.identityhub.identity.domain.UserAccountRef;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class GlobalAccountAdminControllerTest {

    private static final Instant NOW = Instant.parse("2026-08-02T18:00:00Z");
    private static final UUID ACCOUNT_ID =
            UUID.fromString("f20ace9e-e02a-436f-93e6-edaff0320733");

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void mapsAuthenticatedAdministratorAndReturnsOnlyOperationState() {
        var useCase = org.mockito.Mockito.mock(DisableGlobalAccount.class);
        when(useCase.execute(any())).thenReturn(completedOperation());
        var controller = new GlobalAccountAdminController(
                useCase, Clock.fixed(NOW, ZoneOffset.UTC));
        MDC.put("correlationId", "disable-account-correlation");

        var response = controller.disable(
                ACCOUNT_ID,
                "disable-account-001",
                new GlobalAccountAdminController.DisableAccountRequest(
                        "Confirmed security incident"),
                authentication(NOW.minusSeconds(30)));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().operationId())
                .isEqualTo(completedOperation().operationId());
        assertThat(response.getBody().userAccountRef()).isEqualTo(ACCOUNT_ID);
        assertThat(response.getBody().status()).isEqualTo("COMPLETED");
        verify(useCase).execute(new DisableGlobalAccount.Command(
                new UserAccountRef(ACCOUNT_ID),
                "Confirmed security incident",
                "disable-account-001",
                "admin-subject",
                "disable-account-correlation"));
    }

    @Test
    void rejectsMissingStaleOrImplausiblyFutureAuthenticationTime() {
        var useCase = org.mockito.Mockito.mock(DisableGlobalAccount.class);
        var controller = new GlobalAccountAdminController(
                useCase, Clock.fixed(NOW, ZoneOffset.UTC));
        var request = new GlobalAccountAdminController.DisableAccountRequest(
                "Confirmed security incident");

        assertThatThrownBy(() -> controller.disable(
                        ACCOUNT_ID, "disable-account-002", request, authentication(null)))
                .isInstanceOf(RecentAdminAuthenticationRequiredException.class);
        assertThatThrownBy(() -> controller.disable(
                        ACCOUNT_ID,
                        "disable-account-003",
                        request,
                        authentication(NOW.minusSeconds(301))))
                .isInstanceOf(RecentAdminAuthenticationRequiredException.class);
        assertThatThrownBy(() -> controller.disable(
                        ACCOUNT_ID,
                        "disable-account-004",
                        request,
                        authentication(NOW.plusSeconds(61))))
                .isInstanceOf(RecentAdminAuthenticationRequiredException.class);
    }

    private JwtAuthenticationToken authentication(Instant authenticationTime) {
        var builder = Jwt.withTokenValue("redacted-test-token")
                .header("alg", "RS256")
                .subject("admin-subject")
                .issuedAt(NOW.minusSeconds(60))
                .expiresAt(NOW.plusSeconds(600));
        if (authenticationTime != null) {
            builder.claim("auth_time", authenticationTime);
        }
        return new JwtAuthenticationToken(
                builder.build(),
                List.of(
                        new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"),
                        new SimpleGrantedAuthority("MFA_TOTP")),
                "admin-subject");
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
