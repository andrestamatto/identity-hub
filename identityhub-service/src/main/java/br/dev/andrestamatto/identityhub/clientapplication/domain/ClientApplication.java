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
    private SelfRegistrationPolicy selfRegistrationPolicy;
    private final Instant registeredAt;

    private ClientApplication(
            ClientApplicationId id,
            ApplicationIdentifier identifier,
            DisplayName displayName,
            ClientApplicationState state,
            SelfRegistrationPolicy selfRegistrationPolicy,
            Instant registeredAt) {
        this.id = Objects.requireNonNull(id);
        this.identifier = Objects.requireNonNull(identifier);
        this.displayName = Objects.requireNonNull(displayName);
        this.state = Objects.requireNonNull(state);
        this.selfRegistrationPolicy = Objects.requireNonNull(selfRegistrationPolicy);
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
                SelfRegistrationPolicy.DISABLED,
                clock.instant().truncatedTo(ChronoUnit.MICROS));
    }

    public static ClientApplication reconstitute(
            ClientApplicationId id,
            ApplicationIdentifier identifier,
            DisplayName displayName,
            ClientApplicationState state,
            SelfRegistrationPolicy selfRegistrationPolicy,
            Instant registeredAt) {
        return new ClientApplication(
                id, identifier, displayName, state, selfRegistrationPolicy, registeredAt);
    }

    public static ClientApplication reconstitute(
            ClientApplicationId id,
            ApplicationIdentifier identifier,
            DisplayName displayName,
            ClientApplicationState state,
            Instant registeredAt) {
        return reconstitute(
                id,
                identifier,
                displayName,
                state,
                SelfRegistrationPolicy.DISABLED,
                registeredAt);
    }

    public boolean configureSelfRegistration(SelfRegistrationPolicy policy) {
        Objects.requireNonNull(policy);
        if (selfRegistrationPolicy == policy) {
            return false;
        }
        selfRegistrationPolicy = policy;
        return true;
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
        return configureMachine(clientId, key, new MachineSettings(), clock);
    }

    public ApplicationClient configureMachine(
            ApplicationClientId clientId,
            ApplicationClientKey key,
            MachineSettings settings,
            Clock clock) {
        Objects.requireNonNull(clock);
        return new ApplicationClient(
                clientId,
                id,
                key,
                Objects.requireNonNull(settings),
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

    public SelfRegistrationPolicy selfRegistrationPolicy() {
        return selfRegistrationPolicy;
    }

    public Instant registeredAt() {
        return registeredAt;
    }
}
