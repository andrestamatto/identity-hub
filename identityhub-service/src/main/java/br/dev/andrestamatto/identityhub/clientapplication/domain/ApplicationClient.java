package br.dev.andrestamatto.identityhub.clientapplication.domain;

import java.time.Instant;
import java.util.Objects;

public final class ApplicationClient {

    private final ApplicationClientId id;
    private final ClientApplicationId applicationId;
    private final ApplicationClientKey key;
    private final ApplicationClientSettings settings;
    private final boolean enabled;
    private final Instant configuredAt;

    ApplicationClient(
            ApplicationClientId id,
            ClientApplicationId applicationId,
            ApplicationClientKey key,
            ApplicationClientSettings settings,
            boolean enabled,
            Instant configuredAt) {
        this.id = Objects.requireNonNull(id);
        this.applicationId = Objects.requireNonNull(applicationId);
        this.key = Objects.requireNonNull(key);
        this.settings = Objects.requireNonNull(settings);
        this.enabled = enabled;
        this.configuredAt = Objects.requireNonNull(configuredAt);
    }

    public static ApplicationClient reconstitute(
            ApplicationClientId id,
            ClientApplicationId applicationId,
            ApplicationClientKey key,
            ApplicationClientSettings settings,
            boolean enabled,
            Instant configuredAt) {
        return new ApplicationClient(
                id,
                applicationId,
                key,
                settings,
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
        return settings.type();
    }

    public ApplicationClientSettings settings() {
        return settings;
    }

    public boolean enabled() {
        return enabled;
    }

    public Instant configuredAt() {
        return configuredAt;
    }
}
