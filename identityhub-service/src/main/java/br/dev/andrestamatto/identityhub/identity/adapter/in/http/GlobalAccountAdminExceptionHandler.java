package br.dev.andrestamatto.identityhub.identity.adapter.in.http;

import br.dev.andrestamatto.identityhub.identity.application.GlobalAccountDisableConflictException;
import br.dev.andrestamatto.identityhub.identity.application.GlobalAccountDisableRejectedException;
import br.dev.andrestamatto.identityhub.identity.application.GlobalAccountDisableRejection;
import br.dev.andrestamatto.identityhub.identity.application.GlobalAccountDisableUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = GlobalAccountAdminController.class)
final class GlobalAccountAdminExceptionHandler {

    @ExceptionHandler(RecentAdminAuthenticationRequiredException.class)
    ProblemDetail recentAuthenticationRequired() {
        return problem(
                HttpStatus.FORBIDDEN,
                "Recent authentication required",
                "Authenticate again with MFA before performing this operation");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalidCommand(IllegalArgumentException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid account lifecycle command",
                exception.getMessage());
    }

    @ExceptionHandler(GlobalAccountDisableConflictException.class)
    ProblemDetail idempotencyConflict() {
        return problem(
                HttpStatus.CONFLICT,
                "Idempotency conflict",
                "The idempotency key was already used for another command");
    }

    @ExceptionHandler(GlobalAccountDisableRejectedException.class)
    ProblemDetail operationRejected(GlobalAccountDisableRejectedException exception) {
        if (exception.rejection() == GlobalAccountDisableRejection.ACCOUNT_NOT_FOUND) {
            return problem(
                    HttpStatus.NOT_FOUND,
                    "User account not found",
                    "The requested user account does not exist");
        }
        return problem(
                HttpStatus.CONFLICT,
                "Account disable rejected",
                "The last enabled platform administrator cannot be disabled");
    }

    @ExceptionHandler(GlobalAccountDisableUnavailableException.class)
    ProblemDetail providerUnavailable() {
        return problem(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Account lifecycle temporarily unavailable",
                "The account disable operation could not be completed at this time");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
