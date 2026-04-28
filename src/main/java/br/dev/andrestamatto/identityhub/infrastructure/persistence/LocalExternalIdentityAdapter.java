package br.dev.andrestamatto.identityhub.infrastructure.persistence;

import br.dev.andrestamatto.identityhub.application.ports.LoadExternalIdentity;
import br.dev.andrestamatto.identityhub.domain.model.EncodedPassword;
import br.dev.andrestamatto.identityhub.domain.model.PermissionName;
import br.dev.andrestamatto.identityhub.domain.model.RoleName;
import br.dev.andrestamatto.identityhub.domain.model.User;
import br.dev.andrestamatto.identityhub.infrastructure.persistence.repository.ExternalUserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(
        prefix = "identity-hub.fake-persistence",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class LocalExternalIdentityAdapter implements LoadExternalIdentity {

    private final ExternalUserRepository externalUserRepository;

    public LocalExternalIdentityAdapter(ExternalUserRepository externalUserRepository) {
        this.externalUserRepository = externalUserRepository;
    }

    @Override
    public java.util.Optional<User> findByIdentity(String identityValue) {
        return externalUserRepository.findByEmail(identityValue)
                .map(entity -> new User(
                        entity.getId(),
                        entity.getEmail(),
                        EncodedPassword.from(entity.getEncodedPassword()),
                        splitCsv(entity.getRoles(), RoleName::from),
                        splitCsv(entity.getPermissions(), PermissionName::from)
                ));
    }

    private <T> Set<T> splitCsv(String value, java.util.function.Function<String, T> converter) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(converter)
                .collect(Collectors.toUnmodifiableSet());
    }
}
