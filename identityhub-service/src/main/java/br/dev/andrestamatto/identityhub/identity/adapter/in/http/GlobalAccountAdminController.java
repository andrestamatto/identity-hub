package br.dev.andrestamatto.identityhub.identity.adapter.in.http;

import br.dev.andrestamatto.identityhub.identity.application.DisableGlobalAccount;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/admin/user-accounts")
@ConditionalOnProperty(
        name = "identityhub.keycloak.identity-management.enabled",
        havingValue = "true")
final class GlobalAccountAdminController {

    private static final Duration MAXIMUM_AUTHENTICATION_AGE = Duration.ofMinutes(5);
    private static final Duration MAXIMUM_FUTURE_SKEW = Duration.ofSeconds(60);

    private final DisableGlobalAccount disableGlobalAccount;
    private final Clock clock;

    GlobalAccountAdminController(DisableGlobalAccount disableGlobalAccount, Clock clock) {
        this.disableGlobalAccount = Objects.requireNonNull(disableGlobalAccount);
        this.clock = Objects.requireNonNull(clock);
    }

    @PostMapping("/{userAccountRef}/disable")
    ResponseEntity<DisableAccountResponse> disable(
            @PathVariable UUID userAccountRef,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody DisableAccountRequest request,
            JwtAuthenticationToken authentication) {
        requireRecentAuthentication(authentication);
        var operation = disableGlobalAccount.execute(new DisableGlobalAccount.Command(
                new br.dev.andrestamatto.identityhub.identity.domain.UserAccountRef(
                        userAccountRef),
                request.reason(),
                idempotencyKey,
                authentication.getName(),
                requireCorrelationId()));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new DisableAccountResponse(
                        operation.operationId(),
                        operation.userAccountRef().value(),
                        operation.status().name()));
    }

    private void requireRecentAuthentication(JwtAuthenticationToken authentication) {
        if (authentication == null) {
            throw new RecentAdminAuthenticationRequiredException();
        }
        Instant authenticationTime;
        try {
            authenticationTime = authentication.getToken().getClaimAsInstant("auth_time");
        } catch (RuntimeException exception) {
            throw new RecentAdminAuthenticationRequiredException();
        }
        var now = clock.instant();
        if (authenticationTime == null
                || authenticationTime.isBefore(now.minus(MAXIMUM_AUTHENTICATION_AGE))
                || authenticationTime.isAfter(now.plus(MAXIMUM_FUTURE_SKEW))) {
            throw new RecentAdminAuthenticationRequiredException();
        }
    }

    private String requireCorrelationId() {
        var correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.isBlank()) {
            throw new IllegalStateException("Correlation id is unavailable");
        }
        return correlationId;
    }

    record DisableAccountRequest(String reason) {
    }

    record DisableAccountResponse(
            UUID operationId,
            UUID userAccountRef,
            String status) {
    }
}
