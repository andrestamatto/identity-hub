package br.dev.andrestamatto.identityhub.communication.adapter.in.http;

import br.dev.andrestamatto.identityhub.communication.application.EmailDeliveryNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = EmailDeliveryAdminController.class)
final class EmailDeliveryAdminExceptionHandler {

    @ExceptionHandler(EmailDeliveryNotFoundException.class)
    ProblemDetail notFound() {
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "The requested email delivery does not exist or cannot be reprocessed");
        problem.setTitle("Email delivery not found");
        return problem;
    }
}
