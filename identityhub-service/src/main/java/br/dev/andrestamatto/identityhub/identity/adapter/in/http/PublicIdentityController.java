package br.dev.andrestamatto.identityhub.identity.adapter.in.http;

import br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationSnapshot;
import br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationUnavailableException;
import br.dev.andrestamatto.identityhub.clientapplication.application.GetClientApplicationByIdentifier;
import br.dev.andrestamatto.identityhub.identity.application.BeginLocalRegistration;
import br.dev.andrestamatto.identityhub.identity.application.ConfirmEmailVerification;
import br.dev.andrestamatto.identityhub.identity.application.CompletePasswordRecovery;
import br.dev.andrestamatto.identityhub.identity.application.EmailVerificationRejectedException;
import br.dev.andrestamatto.identityhub.identity.application.PasswordRecoveryRejectedException;
import br.dev.andrestamatto.identityhub.identity.application.RequestPasswordRecovery;
import br.dev.andrestamatto.identityhub.identity.domain.LoginEmail;
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
    static final String RECOVERY_ACCEPTED_MESSAGE =
            "If the account is eligible, password recovery instructions will be sent";

    private final GetClientApplicationByIdentifier getApplication;
    private final BeginLocalRegistration beginRegistration;
    private final ConfirmEmailVerification confirmVerification;
    private final RequestPasswordRecovery requestPasswordRecovery;
    private final CompletePasswordRecovery completePasswordRecovery;
    private final InMemoryRegistrationRateLimiter registrationRateLimiter;
    private final InMemoryPasswordRecoveryRateLimiter recoveryRateLimiter;
    private final PublicResponseTiming responseTiming;

    PublicIdentityController(
            GetClientApplicationByIdentifier getApplication,
            BeginLocalRegistration beginRegistration,
            ConfirmEmailVerification confirmVerification,
            RequestPasswordRecovery requestPasswordRecovery,
            CompletePasswordRecovery completePasswordRecovery,
            InMemoryRegistrationRateLimiter registrationRateLimiter,
            InMemoryPasswordRecoveryRateLimiter recoveryRateLimiter,
            PublicResponseTiming responseTiming) {
        this.getApplication = Objects.requireNonNull(getApplication);
        this.beginRegistration = Objects.requireNonNull(beginRegistration);
        this.confirmVerification = Objects.requireNonNull(confirmVerification);
        this.requestPasswordRecovery = Objects.requireNonNull(requestPasswordRecovery);
        this.completePasswordRecovery = Objects.requireNonNull(completePasswordRecovery);
        this.registrationRateLimiter = Objects.requireNonNull(registrationRateLimiter);
        this.recoveryRateLimiter = Objects.requireNonNull(recoveryRateLimiter);
        this.responseTiming = Objects.requireNonNull(responseTiming);
    }

    @PostMapping("/password-recoveries")
    ResponseEntity<Void> completePasswordRecovery(
            @RequestBody CompletePasswordRecoveryRequest request) {
        if (request == null) {
            throw new PasswordRecoveryRejectedException();
        }
        try (request) {
            var password = request.passwordCopy();
            try {
                completePasswordRecovery.execute(new CompletePasswordRecovery.Command(
                        recoveryToken(request.token()), password, correlationId()));
                return ResponseEntity.noContent()
                        .cacheControl(CacheControl.noStore())
                        .build();
            } finally {
                Arrays.fill(password, '\0');
            }
        }
    }

    private String recoveryToken(String token) {
        if (token == null || token.isBlank()) {
            throw new PasswordRecoveryRejectedException();
        }
        return token;
    }

    @PostMapping("/applications/{applicationIdentifier}/password-recoveries")
    ResponseEntity<RegistrationAcceptedResponse> recoverPassword(
            @PathVariable String applicationIdentifier,
            @RequestBody PasswordRecoveryRequest request,
            HttpServletRequest servletRequest) {
        recoveryRateLimiter.acquire(servletRequest.getRemoteAddr());
        var email = recoveryEmail(request);
        try (var ignored = responseTiming.begin()) {
            var application = recoveryApplication(applicationIdentifier);
            requestPasswordRecovery.execute(new RequestPasswordRecovery.Command(
                    application.applicationId(), email, correlationId()));
            return ResponseEntity.accepted()
                    .cacheControl(CacheControl.noStore())
                    .body(new RegistrationAcceptedResponse(RECOVERY_ACCEPTED_MESSAGE));
        }
    }

    private ClientApplicationSnapshot recoveryApplication(String identifier) {
        try {
            return getApplication.execute(identifier);
        } catch (ClientApplicationUnavailableException exception) {
            throw new PasswordRecoveryUnavailableException();
        }
    }

    private String recoveryEmail(PasswordRecoveryRequest request) {
        try {
            if (request == null || request.email() == null) {
                throw new IllegalArgumentException("Missing email");
            }
            return new LoginEmail(request.email()).contactValue();
        } catch (IllegalArgumentException exception) {
            throw new InvalidPasswordRecoveryRequestException();
        }
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

    record PasswordRecoveryRequest(String email) {
        @Override
        public String toString() {
            return "PasswordRecoveryRequest[email=REDACTED]";
        }
    }

    record EmailVerificationRequest(String token) {
        @Override
        public String toString() {
            return "EmailVerificationRequest[token=REDACTED]";
        }
    }

    record CompletePasswordRecoveryRequest(String token, char[] newPassword)
            implements AutoCloseable {

        char[] passwordCopy() {
            if (newPassword == null) {
                return new char[0];
            }
            return newPassword.clone();
        }

        @Override
        public void close() {
            if (newPassword != null) {
                Arrays.fill(newPassword, '\0');
            }
        }

        @Override
        public String toString() {
            return "CompletePasswordRecoveryRequest[credentials=REDACTED]";
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
