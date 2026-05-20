package br.dev.andrestamatto.identityhub.application.ports.output;

import br.dev.andrestamatto.identityhub.domain.entities.User;
import br.dev.andrestamatto.identityhub.domain.valueobjects.Username;

public interface UserRepository {
    boolean existsBy(Username username);
    User save(User user);
    User findByUsername(Username username);
}
