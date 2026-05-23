package br.dev.andrestamatto.identityhub.infrastructure.repository.mapper;

import br.dev.andrestamatto.identityhub.domain.entities.User;
import br.dev.andrestamatto.identityhub.domain.valueobjects.EncodedPassword;
import br.dev.andrestamatto.identityhub.domain.valueobjects.Username;
import br.dev.andrestamatto.identityhub.domain.valueobjects.UsernameType;
import br.dev.andrestamatto.identityhub.infrastructure.repository.entity.UserJpaEntity;

import java.util.Optional;

public final class UserEntityMapper {

    public static UserJpaEntity jpaEntityFrom(User user) {
        return Optional.ofNullable(user)
                .map((validUser) -> {
                    return UserJpaEntity.of(
                        validUser.uuid(),
                        validUser.username().value(),
                        validUser.username().usernameType().name(),
                        validUser.encodedPassword().value(),
                        validUser.status(),
                        validUser.createdAt(),
                        validUser.updatedAt()
                    );
                })
                .orElseThrow();
    }

    public static User toDomain(UserJpaEntity jpaUserEntity) {
        return Optional.ofNullable(jpaUserEntity)
                .map( (validJpaUserEntity) -> {
                    return User.fromPersistence(
                            validJpaUserEntity.getId(),
                            Username.create(
                                    validJpaUserEntity.getUsername(),
                                    UsernameType.valueOf(validJpaUserEntity.getUsernameType())
                            ),
                            EncodedPassword.create(validJpaUserEntity.getEncodedPassword()),
                            validJpaUserEntity.getStatus(),
                            validJpaUserEntity.getCreatedAt(),
                            validJpaUserEntity.getUpdatedAt()
                    );
                })
                .orElseThrow();
    }

}
