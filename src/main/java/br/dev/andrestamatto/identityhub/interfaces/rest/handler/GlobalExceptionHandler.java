package br.dev.andrestamatto.identityhub.interfaces.rest.handler;

import br.dev.andrestamatto.identityhub.application.exception.AuthenticationFailedException;
import br.dev.andrestamatto.identityhub.application.exception.IdentitySourceUnavailableException;
import br.dev.andrestamatto.identityhub.interfaces.rest.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationFailed(
            AuthenticationFailedException exception,
            HttpServletRequest request
    ) {
        return this.buildResponse(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
    }

    @ExceptionHandler(IdentitySourceUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleIdentitySourceUnavailable(
            IdentitySourceUnavailableException exception,
            HttpServletRequest request
    ) {
        return this.buildResponse(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("Invalid request.");

        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ApiErrorResponse> handleNotImplementedException(
            UnsupportedOperationException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.NOT_IMPLEMENTED, exception.getMessage(), request);
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
}
