package br.dev.andrestamatto.identityhub.bootstrap.security;

import br.dev.andrestamatto.identityhub.audit.application.AdministrativeAccessAttempt;
import br.dev.andrestamatto.identityhub.audit.application.AdministrativeAccessAudit;
import br.dev.andrestamatto.identityhub.audit.application.AdministrativeAccessOutcome;
import br.dev.andrestamatto.identityhub.bootstrap.observability.CorrelationIdFilter;
import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
final class AdminAccessAuditFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminAccessAuditFilter.class);
    private static final String ADMIN_PATH_PREFIX = "/internal/admin/";

    private final AdministrativeAccessAudit audit;

    AdminAccessAuditFilter(AdministrativeAccessAudit audit) {
        this.audit = audit;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
            recordSafely(request, response.getStatus());
        } catch (ServletException | IOException | RuntimeException exception) {
            recordSafely(request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            throw exception;
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(ADMIN_PATH_PREFIX);
    }

    private void recordSafely(HttpServletRequest request, int responseStatus) {
        var attempt = new AdministrativeAccessAttempt(
                correlationId(request),
                actorSubject(SecurityContextHolder.getContext().getAuthentication()),
                limit(request.getMethod(), 16),
                limit(request.getRequestURI(), 512),
                outcome(responseStatus),
                reason(responseStatus));
        try {
            audit.record(attempt);
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Administrative access audit persistence failed: correlationId={}, outcome={}, reason={}",
                    attempt.correlationId(),
                    attempt.outcome(),
                    attempt.reason(),
                    exception);
        }
    }

    private String correlationId(HttpServletRequest request) {
        var value = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        return value instanceof String correlationId
                ? limit(correlationId, 64)
                : "unavailable";
    }

    private String actorSubject(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return limit(authentication.getName(), 255);
    }

    private AdministrativeAccessOutcome outcome(int status) {
        return status < HttpServletResponse.SC_BAD_REQUEST
                ? AdministrativeAccessOutcome.ALLOWED
                : AdministrativeAccessOutcome.DENIED;
    }

    private String reason(int status) {
        return switch (status) {
            case HttpServletResponse.SC_UNAUTHORIZED -> "unauthenticated";
            case HttpServletResponse.SC_FORBIDDEN -> "access_denied";
            default -> status < HttpServletResponse.SC_BAD_REQUEST
                    ? "authorized"
                    : "request_failed";
        };
    }

    private String limit(String value, int maximumLength) {
        return value.length() <= maximumLength
                ? value
                : value.substring(0, maximumLength);
    }
}
