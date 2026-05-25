package br.dev.andrestamatto.identityhub.application.exceptions;

public class UnsupportedVerificationMethodForUsernameTypeException extends RuntimeException {
    public UnsupportedVerificationMethodForUsernameTypeException(String message) {
        super(message);
    }
}
