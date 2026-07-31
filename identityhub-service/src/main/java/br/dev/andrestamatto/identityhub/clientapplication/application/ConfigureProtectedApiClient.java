package br.dev.andrestamatto.identityhub.clientapplication.application;

import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientKey;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplication;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplicationId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.TokenAudience;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public final class ConfigureProtectedApiClient {

    private final ClientApplicationRepository applicationRepository;
    private final ApplicationClientConfigurationRepository clientRepository;
    private final Clock clock;
    private final Supplier<UUID> operationIdGenerator;

    public ConfigureProtectedApiClient(
            ClientApplicationRepository applicationRepository,
            ApplicationClientConfigurationRepository clientRepository,
            Clock clock,
            Supplier<UUID> operationIdGenerator) {
        this.applicationRepository = Objects.requireNonNull(applicationRepository);
        this.clientRepository = Objects.requireNonNull(clientRepository);
        this.clock = Objects.requireNonNull(clock);
        this.operationIdGenerator = Objects.requireNonNull(operationIdGenerator);
    }

    public ApplicationClientConfigurationResult execute(Command command) {
        Objects.requireNonNull(command);
        var applicationId = new ClientApplicationId(command.applicationId());
        var clientId = new ApplicationClientId(command.applicationClientId());
        var key = new ApplicationClientKey(command.key());
        var audience = new TokenAudience(command.audience());
        var application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ClientApplicationNotFoundException(
                        applicationId.value()));

        var existing = clientRepository.findById(clientId);
        if (existing.isPresent()) {
            return replay(existing.orElseThrow(), applicationId, key, audience);
        }
        rejectAssignedKey(applicationId, key);
        rejectAssignedAudience(audience);

        return create(application, clientId, key, audience, command.correlationId());
    }

    private ApplicationClientConfigurationResult create(
            ClientApplication application,
            ApplicationClientId clientId,
            ApplicationClientKey key,
            TokenAudience audience,
            String correlationId) {
        var client = application.configureProtectedApi(clientId, key, audience, clock);
        var now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        var projection = ApplicationClientProjection.pending(
                operationIdGenerator.get(), clientId, correlationId, now);
        var configuration = new ApplicationClientConfiguration(client, projection);
        clientRepository.add(configuration);
        return new ApplicationClientConfigurationResult(
                ApplicationClientSnapshot.from(configuration), true);
    }

    private ApplicationClientConfigurationResult replay(
            ApplicationClientConfiguration existing,
            ClientApplicationId applicationId,
            ApplicationClientKey key,
            TokenAudience audience) {
        var client = existing.client();
        if (client.applicationId().equals(applicationId)
                && client.key().equals(key)
                && client.audience().equals(audience)) {
            return new ApplicationClientConfigurationResult(
                    ApplicationClientSnapshot.from(existing), false);
        }
        throw new ClientApplicationConflictException(
                "Application client id is already assigned to different content");
    }

    private void rejectAssignedKey(
            ClientApplicationId applicationId,
            ApplicationClientKey key) {
        if (clientRepository.findByKey(applicationId, key).isPresent()) {
            throw new ClientApplicationConflictException(
                    "Application client key is already assigned inside the application");
        }
    }

    private void rejectAssignedAudience(TokenAudience audience) {
        if (clientRepository.findByAudience(audience).isPresent()) {
            throw new ClientApplicationConflictException(
                    "Application client audience is already assigned in the environment");
        }
    }

    public record Command(
            UUID applicationId,
            UUID applicationClientId,
            String key,
            String audience,
            String correlationId) {
    }
}
