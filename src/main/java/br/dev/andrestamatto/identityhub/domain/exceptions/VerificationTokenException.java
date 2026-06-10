package br.dev.andrestamatto.identityhub.domain.exceptions;

public class VerificationTokenException extends RuntimeException {
    public VerificationTokenException(String message) {
        super(message);
    }
}
