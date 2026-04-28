package br.dev.andrestamatto.identityhub.infrastructure.mappers;

import br.dev.andrestamatto.identityhub.domain.model.ExternalUser;
import br.dev.andrestamatto.identityhub.domain.model.User;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class UserMapper {

    public User toUser(ExternalUser externalUser) {
        return new User(
                externalUser.userId(),
                externalUser.identity(),
                externalUser.encodedPassword(),
                externalUser.roles(),
                externalUser.permissions()
        );
    }

    public ExternalUser toExternalUser(User user) {
        return new ExternalUser(
                user.getId(),
                user.getIdentity(),
                user.getEncodedPassword(),
                Set.copyOf(user.getRoles()),
                Set.copyOf(user.getPermissions())
        );
    }

}
