package br.dev.andrestamatto.identityhub.clientapplication.application;

import java.util.Objects;
import java.util.UUID;

public record ApplicationTokenClient(UUID id, String type, String audience) {

    public ApplicationTokenClient {
        Objects.requireNonNull(id);
        Objects.requireNonNull(type);
        if (!(type.equals("API") || type.equals("SPA") || type.equals("BFF"))) {
            throw new IllegalArgumentException("Unsupported public token client type");
        }
        if (type.equals("API") != (audience != null && !audience.isBlank())) {
            throw new IllegalArgumentException("Only API clients require an audience");
        }
    }

    public boolean isApi() {
        return type.equals("API");
    }

    public boolean isBrowser() {
        return type.equals("SPA") || type.equals("BFF");
    }
}
