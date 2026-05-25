package br.dev.andrestamatto.identityhub.application.exceptions;

public class UserStatusDoesNotMatchRegistrationConfirmationException extends RuntimeException {

    public UserStatusDoesNotMatchRegistrationConfirmationException() {
        super("User status doesn't match registration confirmation condition");
    }

    public UserStatusDoesNotMatchRegistrationConfirmationException(String message) {
        super(message);
    }
}
