package br.dev.andrestamatto.identityhub.application.exceptions;

public class VerificationTokenException extends RuntimeException {
    public VerificationTokenException(String message) {
        super(message);
    }
}
