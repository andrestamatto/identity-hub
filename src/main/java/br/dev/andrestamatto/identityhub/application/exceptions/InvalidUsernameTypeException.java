package br.dev.andrestamatto.identityhub.application.exceptions;

public class InvalidUsernameTypeException extends RuntimeException {

    public InvalidUsernameTypeException() {
        super("Invalid username type.");
    }

    public InvalidUsernameTypeException(String message) {
        super(message);
    }
}
