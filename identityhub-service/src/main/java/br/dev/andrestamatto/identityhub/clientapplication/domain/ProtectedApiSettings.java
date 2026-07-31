package br.dev.andrestamatto.identityhub.clientapplication.domain;

import java.util.Objects;

public record ProtectedApiSettings(TokenAudience audience)
        implements ApplicationClientSettings {

    public ProtectedApiSettings {
        Objects.requireNonNull(audience);
    }

    @Override
    public ApplicationClientType type() {
        return ApplicationClientType.API;
    }
}
