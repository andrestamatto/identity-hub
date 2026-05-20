package br.dev.andrestamatto.identityhub.infrastructure.repository.mapper;

import br.dev.andrestamatto.identityhub.domain.entities.User;
import br.dev.andrestamatto.identityhub.infrastructure.repository.entity.UserJpaEntity;

import java.util.Optional;

public class UserMapper {

    public UserJpaEntity userJpaEntityFrom(User user) {
        return Optional.ofNullable(user)
                .map((validUser) ->{
                    new UserJpaEntity(
                      user.uuid(),
                      user.username().value(),
                      user.encodedPassword().value(),
                      user.status(),
                      user.createdAt()
                    );
                })
                .orElseThrow();
    }

}
