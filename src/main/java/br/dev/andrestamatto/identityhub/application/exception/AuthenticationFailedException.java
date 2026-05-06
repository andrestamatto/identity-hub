package br.dev.andrestamatto.identityhub.application.exception;

public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException() {
        super("Invalid identity or password");
    }

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
