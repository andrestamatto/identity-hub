package br.dev.andrestamatto.identityhub.clientapplication.adapter.in.http;

import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientNotFoundException;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientProjectionException;
import br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationConflictException;
import br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {
    ClientApplicationAdminController.class,
    BffClientSecretAdminController.class
})
final class ClientApplicationAdminExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalidApplication(IllegalArgumentException exception) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid client application",
                exception.getMessage());
    }

    @ExceptionHandler(ClientApplicationNotFoundException.class)
    ProblemDetail applicationNotFound() {
        return problem(
                HttpStatus.NOT_FOUND,
                "Client application not found",
                "The requested client application does not exist");
    }

    @ExceptionHandler(ClientApplicationConflictException.class)
    ProblemDetail applicationConflict() {
        return problem(
                HttpStatus.CONFLICT,
                "Client application conflict",
                "The application or client identity is already assigned");
    }

    @ExceptionHandler(ApplicationClientNotFoundException.class)
    ProblemDetail applicationClientNotFound() {
        return problem(
                HttpStatus.NOT_FOUND,
                "Application client not found",
                "The requested application client does not exist");
    }

    @ExceptionHandler(ApplicationClientProjectionException.class)
    ProblemDetail credentialProviderFailure(ApplicationClientProjectionException exception) {
        return problem(
                exception.retryable() ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_GATEWAY,
                "Client credential provider unavailable",
                "The client credential operation could not be completed");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
