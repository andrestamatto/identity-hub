package br.dev.andrestamatto.identityhub.infrastructure.persistence;

import br.dev.andrestamatto.identityhub.application.ports.LoadExternalIdentity;
import br.dev.andrestamatto.identityhub.domain.model.ExternalUser;
import br.dev.andrestamatto.identityhub.infrastructure.persistence.repository.ExternalUserRepository;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class LocalExternalIdentityAdapter implements LoadExternalIdentity {

    private final ExternalUserRepository externalUserRepository;

    public LocalExternalIdentityAdapter(ExternalUserRepository externalUserRepository) {
        this.externalUserRepository = externalUserRepository;
    }

    @Override
    public java.util.Optional<ExternalUser> findByEmail(String emailValue) {
        return externalUserRepository.findByEmail(emailValue)
                .map(entity -> new ExternalUser(
                        entity.getId(),
                        entity.getEmail(),
                        entity.getEncodedPassword(),
                        splitCsv(entity.getRoles()),
                        splitCsv(entity.getPermissions())
                ));
    }

    private Set<String> splitCsv(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
