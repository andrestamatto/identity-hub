package br.dev.andrestamatto.identityhub.infrastructure.repository;

import br.dev.andrestamatto.identityhub.infrastructure.repository.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, UUID> {
    boolean existsByUsername(String username);
    Optional<UserJpaEntity> findByUsername(String username);
}
