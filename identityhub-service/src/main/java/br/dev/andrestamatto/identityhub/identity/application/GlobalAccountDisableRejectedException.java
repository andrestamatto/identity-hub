package br.dev.andrestamatto.identityhub.identity.application;

import java.util.Objects;

public final class GlobalAccountDisableRejectedException extends RuntimeException {

    private final GlobalAccountDisableRejection rejection;

    public GlobalAccountDisableRejectedException(GlobalAccountDisableRejection rejection) {
        super("Global account disable operation was rejected");
        this.rejection = Objects.requireNonNull(rejection);
    }

    public GlobalAccountDisableRejection rejection() {
        return rejection;
    }
}
