package br.dev.andrestamatto.identityhub.interfaces.rest.response;

import java.time.Instant;

public record ApiErrorResponse(
        Instant timestamp,
        int httpStatus,
        String httpError,
        String message,
        String path
) {
}
