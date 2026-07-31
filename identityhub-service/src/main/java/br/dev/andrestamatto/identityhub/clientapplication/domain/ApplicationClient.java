package br.dev.andrestamatto.identityhub.clientapplication.domain;

import java.time.Instant;
import java.util.Objects;

public final class ApplicationClient {

    private final ApplicationClientId id;
    private final ClientApplicationId applicationId;
    private final ApplicationClientKey key;
    private final ApplicationClientType type;
    private final TokenAudience audience;
    private final boolean enabled;
    private final Instant configuredAt;

    ApplicationClient(
            ApplicationClientId id,
            ClientApplicationId applicationId,
            ApplicationClientKey key,
            ApplicationClientType type,
            TokenAudience audience,
            boolean enabled,
            Instant configuredAt) {
        this.id = Objects.requireNonNull(id);
        this.applicationId = Objects.requireNonNull(applicationId);
        this.key = Objects.requireNonNull(key);
        this.type = Objects.requireNonNull(type);
        this.audience = Objects.requireNonNull(audience);
        this.enabled = enabled;
        this.configuredAt = Objects.requireNonNull(configuredAt);
    }

    public static ApplicationClient reconstitute(
            ApplicationClientId id,
            ClientApplicationId applicationId,
            ApplicationClientKey key,
            ApplicationClientType type,
            TokenAudience audience,
            boolean enabled,
            Instant configuredAt) {
        return new ApplicationClient(
                id,
                applicationId,
                key,
                type,
                audience,
                enabled,
                configuredAt);
    }

    public ApplicationClientId id() {
        return id;
    }

    public ClientApplicationId applicationId() {
        return applicationId;
    }

    public ApplicationClientKey key() {
        return key;
    }

    public ApplicationClientType type() {
        return type;
    }

    public TokenAudience audience() {
        return audience;
    }

    public boolean enabled() {
        return enabled;
    }

    public Instant configuredAt() {
        return configuredAt;
    }
}
