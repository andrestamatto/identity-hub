package br.dev.andrestamatto.identityhub.interfaces.rest.handler;

import br.dev.andrestamatto.identityhub.application.exceptions.EmailDeliveryException;
import br.dev.andrestamatto.identityhub.application.exceptions.UserAlreadyExistsException;
import br.dev.andrestamatto.identityhub.application.exceptions.UserNotFoundException;
import br.dev.andrestamatto.identityhub.domain.exceptions.UserStatusDoesNotMatchRegistrationConfirmationException;
import br.dev.andrestamatto.identityhub.interfaces.rest.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({UserAlreadyExistsException.class, UserStatusDoesNotMatchRegistrationConfirmationException.class})
    public ResponseEntity<ApiErrorResponse> handleUserAlreadyExistsException(Exception exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFoundException(Exception exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(EmailDeliveryException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailDeliveryException(Exception exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_GATEWAY, publicEmailDeliveryMessage(exception), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(Exception exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(
                        Instant.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        message,
                        request.getRequestURI()
                ));
    }

    private String publicEmailDeliveryMessage(Exception exception) {
        var message = String.valueOf(exception.getMessage()).toLowerCase();

        if (message.contains("timeout") || message.contains("timed out")) {
            return "Email delivery timed out. Please try again later.";
        }

        if (message.contains("unavailable") || message.contains("connection refused") || message.contains("couldn't connect")) {
            return "Email delivery service is unavailable. Please try again later.";
        }

        return "Email delivery failed. Please try again later.";
    }
}
