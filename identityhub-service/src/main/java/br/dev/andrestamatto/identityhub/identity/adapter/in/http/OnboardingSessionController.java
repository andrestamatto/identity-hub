package br.dev.andrestamatto.identityhub.identity.adapter.in.http;

import br.dev.andrestamatto.identityhub.identity.application.BeginOnboardingSession;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/integration/v1/onboarding-sessions")
@ConditionalOnProperty(name = "identityhub.onboarding.enabled", havingValue = "true")
final class OnboardingSessionController {

    private final BeginOnboardingSession beginSession;
    private final OnboardingSessionMetrics metrics;

    OnboardingSessionController(
            BeginOnboardingSession beginSession,
            OnboardingSessionMetrics metrics) {
        this.beginSession = Objects.requireNonNull(beginSession);
        this.metrics = Objects.requireNonNull(metrics);
    }

    @PostMapping
    ResponseEntity<OnboardingSessionResponse> begin(
            Authentication authentication,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody OnboardingSessionRequest request) {
        Objects.requireNonNull(authentication);
        if (request == null) {
            throw new IllegalArgumentException("Onboarding session data is required");
        }
        var command = new BeginOnboardingSession.Command(
                UUID.fromString(authentication.getName()),
                request.browserClientId(),
                request.acquisitionReference(),
                request.redirectUri(),
                request.codeChallenge(),
                idempotencyKey,
                correlationId());
        var result = metrics.record(() -> beginSession.execute(command));
        var response = ResponseEntity.status(result.created() ? 201 : 200)
                .cacheControl(CacheControl.noStore());
        return response.body(new OnboardingSessionResponse(
                result.sessionId(), result.expiresAt()));
    }

    private String correlationId() {
        var correlationId = MDC.get("correlationId");
        return correlationId == null ? UUID.randomUUID().toString() : correlationId;
    }

    record OnboardingSessionRequest(
            UUID browserClientId,
            String acquisitionReference,
            String redirectUri,
            String codeChallenge) {
    }

    record OnboardingSessionResponse(String onboardingSession, Instant expiresAt) {
    }
}
