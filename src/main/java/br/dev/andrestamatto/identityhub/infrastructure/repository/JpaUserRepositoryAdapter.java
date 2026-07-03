package br.dev.andrestamatto.identityhub.infrastructure.repository;

import br.dev.andrestamatto.identityhub.application.ports.output.UserRepository;
import br.dev.andrestamatto.identityhub.domain.entities.User;
import br.dev.andrestamatto.identityhub.domain.valueobjects.Username;
import br.dev.andrestamatto.identityhub.infrastructure.repository.entity.UserEntity;
import br.dev.andrestamatto.identityhub.infrastructure.repository.mapper.UserEntityMapper;
import br.dev.andrestamatto.identityhub.infrastructure.repository.ports.SpringDataUserRepository;

public class JpaUserRepositoryAdapter implements UserRepository {
    private final SpringDataUserRepository repository;

    public JpaUserRepositoryAdapter(SpringDataUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsBy(Username username) {
        return repository.existsByUsername(username.value());
    }

    @Override
    public User save(User user) {
        UserEntity entity = UserEntityMapper.jpaEntityFrom(user);
        UserEntity saved = repository.save(entity);
        return UserEntityMapper.toDomain(saved);
    }

    @Override
    public User findByUsername(Username username) {
        return repository.findByUsername(username.value())
                .map(UserEntityMapper::toDomain)
                .orElse(null);
    }
}
