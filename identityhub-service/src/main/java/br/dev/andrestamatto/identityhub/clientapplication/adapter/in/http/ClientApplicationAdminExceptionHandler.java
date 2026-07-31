package br.dev.andrestamatto.identityhub.clientapplication.adapter.in.http;

import br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationConflictException;
import br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ClientApplicationAdminController.class)
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
                "The application id or identifier is already assigned");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
