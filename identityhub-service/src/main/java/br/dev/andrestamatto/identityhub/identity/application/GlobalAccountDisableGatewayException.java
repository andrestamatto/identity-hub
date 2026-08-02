package br.dev.andrestamatto.identityhub.identity.application;

public final class GlobalAccountDisableGatewayException extends RuntimeException {

    public GlobalAccountDisableGatewayException() {
        super("Global account lifecycle provider is unavailable");
    }

    public GlobalAccountDisableGatewayException(Throwable cause) {
        super("Global account lifecycle provider is unavailable", cause);
    }
}
