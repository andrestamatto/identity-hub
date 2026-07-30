package br.dev.andrestamatto.identityhub.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;

import br.dev.andrestamatto.identityhub.audit.application.AdministrativeAccessAttempt;
import br.dev.andrestamatto.identityhub.audit.application.AdministrativeAccessAudit;
import br.dev.andrestamatto.identityhub.audit.application.AdministrativeAccessOutcome;
import br.dev.andrestamatto.identityhub.bootstrap.observability.CorrelationIdFilter;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AdminAccessAuditFilterTest {

    private final ArrayList<AdministrativeAccessAttempt> attempts = new ArrayList<>();
    private final AdministrativeAccessAudit audit = new AdministrativeAccessAudit(
            event -> attempts.add(new AdministrativeAccessAttempt(
                    event.correlationId(),
                    event.actorSubject(),
                    event.method(),
                    event.path(),
                    event.outcome(),
                    event.reason())),
            java.time.Clock.systemUTC(),
            java.util.UUID::randomUUID);
    private final AdminAccessAuditFilter filter = new AdminAccessAuditFilter(audit);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsAllowedAdministrativeAccessWithoutQueryString() throws Exception {
        var authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        "operator-id",
                        null,
                        java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        var request = new MockHttpServletRequest("GET", "/internal/admin/runtime");
        request.setQueryString("private-data=must-not-be-audited");
        request.setAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE, "correlation-123");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
                response.setStatus(200));

        assertThat(attempts).containsExactly(new AdministrativeAccessAttempt(
                "correlation-123",
                "operator-id",
                "GET",
                "/internal/admin/runtime",
                AdministrativeAccessOutcome.ALLOWED,
                "authorized"));
    }

    @Test
    void recordsDeniedAnonymousAdministrativeAccess() throws Exception {
        var request = new MockHttpServletRequest("GET", "/internal/admin/runtime");
        request.setAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE, "correlation-456");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
                response.setStatus(401));

        assertThat(attempts).containsExactly(new AdministrativeAccessAttempt(
                "correlation-456",
                null,
                "GET",
                "/internal/admin/runtime",
                AdministrativeAccessOutcome.DENIED,
                "unauthenticated"));
    }

    @Test
    void ignoresNonAdministrativeRequests() throws Exception {
        var request = new MockHttpServletRequest("GET", "/actuator/health/liveness");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
                response.setStatus(200));

        assertThat(attempts).isEmpty();
    }

    @Test
    void auditPersistenceFailureDoesNotChangeAuthorizedResponse() throws Exception {
        var failingAudit = new AdministrativeAccessAudit(
                ignoredEvent -> {
                    throw new IllegalStateException("synthetic persistence failure");
                },
                java.time.Clock.systemUTC(),
                java.util.UUID::randomUUID);
        var failingAuditFilter = new AdminAccessAuditFilter(failingAudit);
        var request = new MockHttpServletRequest("GET", "/internal/admin/runtime");
        request.setAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE, "correlation-789");
        var response = new MockHttpServletResponse();

        failingAuditFilter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
                response.setStatus(200));

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
