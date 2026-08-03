package br.dev.andrestamatto.identityhub.clientapplication.adapter.out.keycloak;

import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientConfigurationRepository;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientProjectionState;
import br.dev.andrestamatto.identityhub.clientapplication.application.MembershipProvisioningClient;
import br.dev.andrestamatto.identityhub.clientapplication.application.MembershipProvisioningClientResolver;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationClientType;
import br.dev.andrestamatto.identityhub.clientapplication.domain.MachineClientScope;
import br.dev.andrestamatto.identityhub.clientapplication.domain.MachineSettings;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class KeycloakMembershipProvisioningClientResolver
        implements MembershipProvisioningClientResolver {

    private static final String MACHINE_CLIENT_PREFIX = "ih-machine-";

    private final ApplicationClientConfigurationRepository repository;

    public KeycloakMembershipProvisioningClientResolver(
            ApplicationClientConfigurationRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public Optional<MembershipProvisioningClient> resolve(String authorizedParty) {
        var clientId = projectedClientId(authorizedParty);
        if (clientId.isEmpty()) {
            return Optional.empty();
        }
        return repository.findById(clientId.orElseThrow())
                .filter(configuration -> configuration.client().enabled())
                .filter(configuration -> configuration.client().type()
                        == ApplicationClientType.MACHINE)
                .filter(configuration -> configuration.projection().state()
                        == ApplicationClientProjectionState.APPLIED)
                .filter(configuration -> configuration.client().settings()
                        instanceof MachineSettings machine
                        && machine.scopes().contains(MachineClientScope.MEMBERSHIP_WRITE))
                .map(configuration -> new MembershipProvisioningClient(
                        configuration.client().applicationId().value(),
                        configuration.client().id().value()));
    }

    private Optional<ApplicationClientId> projectedClientId(String authorizedParty) {
        if (authorizedParty == null || !authorizedParty.startsWith(MACHINE_CLIENT_PREFIX)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new ApplicationClientId(UUID.fromString(
                    authorizedParty.substring(MACHINE_CLIENT_PREFIX.length()))));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
