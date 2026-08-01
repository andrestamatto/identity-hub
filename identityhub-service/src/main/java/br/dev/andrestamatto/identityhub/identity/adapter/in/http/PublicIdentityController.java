package br.dev.andrestamatto.identityhub.identity.adapter.in.http;

import br.dev.andrestamatto.identityhub.clientapplication.application.GetClientApplicationByIdentifier;
import br.dev.andrestamatto.identityhub.identity.application.BeginLocalRegistration;
import br.dev.andrestamatto.identityhub.identity.application.ConfirmEmailVerification;
import br.dev.andrestamatto.identityhub.identity.application.EmailVerificationRejectedException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/v1")
@ConditionalOnProperty(name = "identityhub.public-identity.enabled", havingValue = "true")
final class PublicIdentityController {

    static final String ACCEPTED_MESSAGE =
            "If the request is eligible, verification instructions will be sent";

    private final GetClientApplicationByIdentifier getApplication;
    private final BeginLocalRegistration beginRegistration;
    private final ConfirmEmailVerification confirmVerification;
    private final InMemoryRegistrationRateLimiter registrationRateLimiter;
    private final PublicResponseTiming responseTiming;

    PublicIdentityController(
            GetClientApplicationByIdentifier getApplication,
            BeginLocalRegistration beginRegistration,
            ConfirmEmailVerification confirmVerification,
            InMemoryRegistrationRateLimiter registrationRateLimiter,
            PublicResponseTiming responseTiming) {
        this.getApplication = Objects.requireNonNull(getApplication);
        this.beginRegistration = Objects.requireNonNull(beginRegistration);
        this.confirmVerification = Objects.requireNonNull(confirmVerification);
        this.registrationRateLimiter = Objects.requireNonNull(registrationRateLimiter);
        this.responseTiming = Objects.requireNonNull(responseTiming);
    }

    @PostMapping("/applications/{applicationIdentifier}/local-registrations")
    ResponseEntity<RegistrationAcceptedResponse> register(
            @PathVariable String applicationIdentifier,
            @RequestBody LocalRegistrationRequest request,
            HttpServletRequest servletRequest) {
        registrationRateLimiter.acquire(servletRequest.getRemoteAddr());
        if (request == null) {
            throw new IllegalArgumentException("Required registration data is missing");
        }
        try (request; var ignored = responseTiming.begin()) {
            var password = request.passwordCopy();
            try {
                var application = getApplication.execute(applicationIdentifier);
                beginRegistration.execute(new BeginLocalRegistration.Command(
                        application.applicationId(),
                        required(request.email()),
                        password,
                        correlationId()));
                return accepted();
            } finally {
                Arrays.fill(password, '\0');
            }
        }
    }

    @PostMapping("/email-verifications")
    ResponseEntity<Void> verify(@RequestBody EmailVerificationRequest request) {
        if (request == null || request.token() == null || request.token().isBlank()) {
            throw new EmailVerificationRejectedException();
        }
        confirmVerification.execute(request.token());
        return ResponseEntity.noContent()
                .cacheControl(CacheControl.noStore())
                .build();
    }

    private ResponseEntity<RegistrationAcceptedResponse> accepted() {
        return ResponseEntity.accepted()
                .cacheControl(CacheControl.noStore())
                .body(new RegistrationAcceptedResponse(ACCEPTED_MESSAGE));
    }

    private String correlationId() {
        var correlationId = MDC.get("correlationId");
        return correlationId == null ? UUID.randomUUID().toString() : correlationId;
    }

    private String required(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required registration data is missing");
        }
        return value;
    }

    record RegistrationAcceptedResponse(String message) { }

    record EmailVerificationRequest(String token) {
        @Override
        public String toString() {
            return "EmailVerificationRequest[token=REDACTED]";
        }
    }

    record LocalRegistrationRequest(String email, char[] password) implements AutoCloseable {

        char[] passwordCopy() {
            if (password == null || password.length == 0) {
                throw new IllegalArgumentException("Required registration data is missing");
            }
            return password.clone();
        }

        @Override
        public void close() {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }

        @Override
        public String toString() {
            return "LocalRegistrationRequest[credentials=REDACTED]";
        }
    }
}
