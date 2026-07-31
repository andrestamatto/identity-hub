package br.dev.andrestamatto.identityhub.clientapplication.application;

import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientKey;
import br.dev.andrestamatto.identityhub.clientapplication.domain.BffSettings;
import br.dev.andrestamatto.identityhub.clientapplication.domain.BrowserTransportPolicy;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplication;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplicationId;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public final class ConfigureBffClient {

    private final ClientApplicationRepository applicationRepository;
    private final ApplicationClientConfigurationRepository clientRepository;
    private final BrowserTransportPolicy transportPolicy;
    private final Clock clock;
    private final Supplier<UUID> operationIdGenerator;

    public ConfigureBffClient(
            ClientApplicationRepository applicationRepository,
            ApplicationClientConfigurationRepository clientRepository,
            BrowserTransportPolicy transportPolicy,
            Clock clock,
            Supplier<UUID> operationIdGenerator) {
        this.applicationRepository = Objects.requireNonNull(applicationRepository);
        this.clientRepository = Objects.requireNonNull(clientRepository);
        this.transportPolicy = Objects.requireNonNull(transportPolicy);
        this.clock = Objects.requireNonNull(clock);
        this.operationIdGenerator = Objects.requireNonNull(operationIdGenerator);
    }

    public ApplicationClientConfigurationResult execute(Command command) {
        Objects.requireNonNull(command);
        var applicationId = new ClientApplicationId(command.applicationId());
        var clientId = new ApplicationClientId(command.applicationClientId());
        var key = new ApplicationClientKey(command.key());
        var settings = BffSettings.create(command.redirectUris(), transportPolicy);
        var application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ClientApplicationNotFoundException(applicationId.value()));

        var existing = clientRepository.findById(clientId);
        if (existing.isPresent()) {
            return replay(existing.orElseThrow(), applicationId, key, settings);
        }
        if (clientRepository.findByKey(applicationId, key).isPresent()) {
            throw new ClientApplicationConflictException(
                    "Application client key is already assigned inside the application");
        }
        return create(application, clientId, key, settings, command.correlationId());
    }

    private ApplicationClientConfigurationResult create(
            ClientApplication application,
            ApplicationClientId clientId,
            ApplicationClientKey key,
            BffSettings settings,
            String correlationId) {
        var client = application.configureBff(clientId, key, settings, clock);
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
            BffSettings settings) {
        var client = existing.client();
        if (client.applicationId().equals(applicationId)
                && client.key().equals(key)
                && client.settings().equals(settings)) {
            return new ApplicationClientConfigurationResult(
                    ApplicationClientSnapshot.from(existing), false);
        }
        throw new ClientApplicationConflictException(
                "Application client id is already assigned to different content");
    }

    public record Command(
            UUID applicationId,
            UUID applicationClientId,
            String key,
            List<String> redirectUris,
            String correlationId) {
    }
}
