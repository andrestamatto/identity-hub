package br.dev.andrestamatto.identityhub.infrastructure.mappers;

import br.dev.andrestamatto.identityhub.domain.model.ExternalUser;
import br.dev.andrestamatto.identityhub.domain.model.Password;
import br.dev.andrestamatto.identityhub.domain.model.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserMapper {

    public User toUser(ExternalUser externalUser) {
        return new User(
                externalUser.userId(),
                externalUser.email(),
                Password.encoded(externalUser.encodedPassword()),
                externalUser.roles(),
                externalUser.permissions()
        );
    }

    public ExternalUser toExternalUser(User user) {
        return new ExternalUser(
                user.getId(),
                user.getEmail(),
                user.getPassword().getValue(),
                user.getRoles().stream().collect(Collectors.toUnmodifiableSet()),
                user.getPermissions().stream().collect(Collectors.toUnmodifiableSet())
        );
    }

}
