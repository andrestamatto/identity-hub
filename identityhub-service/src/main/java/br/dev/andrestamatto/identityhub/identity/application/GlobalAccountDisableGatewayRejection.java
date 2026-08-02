package br.dev.andrestamatto.identityhub.identity.application;

import java.util.Objects;

public final class GlobalAccountDisableGatewayRejection extends RuntimeException {

    private final GlobalAccountDisableRejection rejection;

    public GlobalAccountDisableGatewayRejection(GlobalAccountDisableRejection rejection) {
        super("Global account disable request was rejected");
        this.rejection = Objects.requireNonNull(rejection);
    }

    public GlobalAccountDisableRejection rejection() {
        return rejection;
    }
}
