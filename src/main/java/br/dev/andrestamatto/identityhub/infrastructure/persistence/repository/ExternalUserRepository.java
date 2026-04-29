package br.dev.andrestamatto.identityhub.infrastructure.persistence.repository;

import br.dev.andrestamatto.identityhub.infrastructure.persistence.entity.ExternalUserEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

@ConditionalOnProperty(
        prefix = "identity-hub.local-identity-store",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public interface ExternalUserRepository extends JpaRepository<ExternalUserEntity, UUID> {
    Optional<ExternalUserEntity> findByEmail(String email);
}
