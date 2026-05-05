package br.dev.andrestamatto.identityhub.application.exception;

public class IdentitySourceUnavailableException extends RuntimeException {

    public IdentitySourceUnavailableException() {
        super("No LoadExternalIdentity implementation is available. Configure an identity source (built-in local store or custom adapter).");
    }

    public IdentitySourceUnavailableException(String message) {
        super(message);
    }

    public IdentitySourceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
