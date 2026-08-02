package br.dev.andrestamatto.identityhub.identity.adapter.in.http;

import br.dev.andrestamatto.identityhub.clientapplication.application.OnboardingOriginRejectedException;
import br.dev.andrestamatto.identityhub.identity.application.OnboardingSessionConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = OnboardingSessionController.class)
final class OnboardingSessionExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalidRequest() {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid onboarding request",
                "The onboarding request is invalid");
    }

    @ExceptionHandler(OnboardingOriginRejectedException.class)
    ProblemDetail forbiddenOrigin() {
        return problem(
                HttpStatus.FORBIDDEN,
                "Onboarding origin rejected",
                "The authenticated client cannot initiate this onboarding session");
    }

    @ExceptionHandler(OnboardingSessionConflictException.class)
    ProblemDetail idempotencyConflict() {
        return problem(
                HttpStatus.CONFLICT,
                "Onboarding session conflict",
                "The idempotency key was already used for a different request");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
