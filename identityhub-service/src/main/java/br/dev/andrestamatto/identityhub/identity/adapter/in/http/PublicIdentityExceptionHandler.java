package br.dev.andrestamatto.identityhub.identity.adapter.in.http;

import br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationUnavailableException;
import br.dev.andrestamatto.identityhub.identity.application.EmailVerificationRateLimitException;
import br.dev.andrestamatto.identityhub.identity.application.EmailVerificationRejectedException;
import br.dev.andrestamatto.identityhub.identity.application.LocalIdentityRegistrationException;
import br.dev.andrestamatto.identityhub.identity.application.LocalIdentityVerificationException;
import br.dev.andrestamatto.identityhub.identity.application.PasswordRecoveryIdentityLookupException;
import br.dev.andrestamatto.identityhub.identity.application.PasswordRecoveryRateLimitException;
import br.dev.andrestamatto.identityhub.identity.application.SelfRegistrationDisabledException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice(assignableTypes = PublicIdentityController.class)
final class PublicIdentityExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> unreadableRequest() {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                "The request body does not match the expected schema");
    }

    @ExceptionHandler(EmailVerificationRateLimitException.class)
    ResponseEntity<PublicIdentityController.RegistrationAcceptedResponse>
            destinationLimitReached() {
        return ResponseEntity.accepted()
                .cacheControl(CacheControl.noStore())
                .body(new PublicIdentityController.RegistrationAcceptedResponse(
                        PublicIdentityController.ACCEPTED_MESSAGE));
    }

    @ExceptionHandler(PasswordRecoveryRateLimitException.class)
    ResponseEntity<PublicIdentityController.RegistrationAcceptedResponse>
            recoveryDestinationLimitReached() {
        return ResponseEntity.accepted()
                .cacheControl(CacheControl.noStore())
                .body(new PublicIdentityController.RegistrationAcceptedResponse(
                        PublicIdentityController.RECOVERY_ACCEPTED_MESSAGE));
    }

    @ExceptionHandler(PasswordRecoveryIdentityLookupException.class)
    ResponseEntity<ProblemDetail> recoveryProviderUnavailable() {
        return problem(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Password recovery temporarily unavailable",
                "Password recovery could not be started at this time");
    }

    @ExceptionHandler(PasswordRecoveryUnavailableException.class)
    ResponseEntity<ProblemDetail> recoveryUnavailable() {
        return problem(
                HttpStatus.NOT_FOUND,
                "Password recovery unavailable",
                "Password recovery is unavailable");
    }

    @ExceptionHandler(InvalidPasswordRecoveryRequestException.class)
    ResponseEntity<ProblemDetail> invalidRecoveryRequest() {
        return problem(
                HttpStatus.valueOf(422),
                "Invalid password recovery request",
                "Use a valid email address");
    }

    @ExceptionHandler({
        ClientApplicationUnavailableException.class,
        SelfRegistrationDisabledException.class
    })
    ResponseEntity<ProblemDetail> registrationUnavailable() {
        return problem(
                HttpStatus.NOT_FOUND,
                "Registration unavailable",
                "Registration is unavailable");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> invalidRegistrationData() {
        return problem(
                HttpStatus.valueOf(422),
                "Invalid registration data",
                "Use a valid email and a password between 15 and 64 characters");
    }

    @ExceptionHandler(EmailVerificationRejectedException.class)
    ResponseEntity<ProblemDetail> verificationRejected() {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Email verification rejected",
                "Email verification could not be completed");
    }

    @ExceptionHandler(LocalIdentityRegistrationException.class)
    ResponseEntity<ProblemDetail> identityProviderUnavailable() {
        return problem(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Registration temporarily unavailable",
                "Registration could not be completed at this time");
    }

    @ExceptionHandler(LocalIdentityVerificationException.class)
    ResponseEntity<ProblemDetail> verificationProviderUnavailable() {
        return problem(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Email verification temporarily unavailable",
                "Email verification could not be completed at this time");
    }

    @ExceptionHandler(PublicRegistrationRateLimitException.class)
    ResponseEntity<ProblemDetail> edgeLimitReached(
            PublicRegistrationRateLimitException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.retryAfterSeconds()))
                .cacheControl(CacheControl.noStore())
                .body(problemDetail(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Too many registration requests",
                        "Try the registration request again later"));
    }

    @ExceptionHandler(PublicPasswordRecoveryRateLimitException.class)
    ResponseEntity<ProblemDetail> recoveryEdgeLimitReached(
            PublicPasswordRecoveryRateLimitException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.retryAfterSeconds()))
                .cacheControl(CacheControl.noStore())
                .body(problemDetail(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Too many password recovery requests",
                        "Try the password recovery request again later"));
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status, String title, String detail) {
        return ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore())
                .body(problemDetail(status, title, detail));
    }

    private ProblemDetail problemDetail(HttpStatus status, String title, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
