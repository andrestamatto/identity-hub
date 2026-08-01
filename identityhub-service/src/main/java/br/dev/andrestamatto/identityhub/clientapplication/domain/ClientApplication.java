package br.dev.andrestamatto.identityhub.clientapplication.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public final class ClientApplication {

    private final ClientApplicationId id;
    private final ApplicationIdentifier identifier;
    private final DisplayName displayName;
    private final ClientApplicationState state;
    private final Instant registeredAt;

    private ClientApplication(
            ClientApplicationId id,
            ApplicationIdentifier identifier,
            DisplayName displayName,
            ClientApplicationState state,
            Instant registeredAt) {
        this.id = Objects.requireNonNull(id);
        this.identifier = Objects.requireNonNull(identifier);
        this.displayName = Objects.requireNonNull(displayName);
        this.state = Objects.requireNonNull(state);
        this.registeredAt = Objects.requireNonNull(registeredAt);
    }

    public static ClientApplication register(
            ClientApplicationId id,
            ApplicationIdentifier identifier,
            DisplayName displayName,
            Clock clock) {
        Objects.requireNonNull(clock);
        return new ClientApplication(
                id,
                identifier,
                displayName,
                ClientApplicationState.DRAFT,
                clock.instant().truncatedTo(ChronoUnit.MICROS));
    }

    public static ClientApplication reconstitute(
            ClientApplicationId id,
            ApplicationIdentifier identifier,
            DisplayName displayName,
            ClientApplicationState state,
            Instant registeredAt) {
        return new ClientApplication(id, identifier, displayName, state, registeredAt);
    }

    public ApplicationClient configureProtectedApi(
            ApplicationClientId clientId,
            ApplicationClientKey key,
            TokenAudience audience,
            Clock clock) {
        Objects.requireNonNull(clock);
        return new ApplicationClient(
                clientId,
                id,
                key,
                new ProtectedApiSettings(audience),
                true,
                clock.instant().truncatedTo(ChronoUnit.MICROS));
    }

    public ApplicationClient configureSpa(
            ApplicationClientId clientId,
            ApplicationClientKey key,
            SpaSettings settings,
            Clock clock) {
        Objects.requireNonNull(clock);
        return new ApplicationClient(
                clientId,
                id,
                key,
                settings,
                true,
                clock.instant().truncatedTo(ChronoUnit.MICROS));
    }

    public ApplicationClient configureBff(
            ApplicationClientId clientId,
            ApplicationClientKey key,
            BffSettings settings,
            Clock clock) {
        Objects.requireNonNull(clock);
        return new ApplicationClient(
                clientId,
                id,
                key,
                settings,
                true,
                clock.instant().truncatedTo(ChronoUnit.MICROS));
    }

    public ApplicationClient configureMachine(
            ApplicationClientId clientId,
            ApplicationClientKey key,
            Clock clock) {
        Objects.requireNonNull(clock);
        return new ApplicationClient(
                clientId,
                id,
                key,
                new MachineSettings(),
                true,
                clock.instant().truncatedTo(ChronoUnit.MICROS));
    }

    public ClientApplicationId id() {
        return id;
    }

    public ApplicationIdentifier identifier() {
        return identifier;
    }

    public DisplayName displayName() {
        return displayName;
    }

    public ClientApplicationState state() {
        return state;
    }

    public Instant registeredAt() {
        return registeredAt;
    }
}
