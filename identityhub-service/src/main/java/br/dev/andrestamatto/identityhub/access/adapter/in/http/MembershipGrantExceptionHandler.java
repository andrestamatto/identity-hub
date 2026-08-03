package br.dev.andrestamatto.identityhub.access.adapter.in.http;

import br.dev.andrestamatto.identityhub.access.application.MembershipGrantConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = MembershipGrantController.class)
final class MembershipGrantExceptionHandler {

    @ExceptionHandler(MembershipProvisioningDeniedException.class)
    ProblemDetail denied() {
        return problem(
                HttpStatus.FORBIDDEN,
                "Membership provisioning denied",
                "The authenticated client cannot provision this membership");
    }

    @ExceptionHandler(MembershipGrantConflictException.class)
    ProblemDetail conflict() {
        return problem(
                HttpStatus.CONFLICT,
                "Idempotency conflict",
                "The idempotency key was already used for another command");
    }

    @ExceptionHandler(MembershipOperationNotFoundException.class)
    ProblemDetail notFound() {
        return problem(
                HttpStatus.NOT_FOUND,
                "Membership operation not found",
                "The membership operation was not found");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalid() {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid membership grant",
                "The membership grant request is invalid");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
