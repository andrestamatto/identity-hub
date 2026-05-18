package br.dev.andrestamatto.identityhub.application.ports.input;

import br.dev.andrestamatto.identityhub.domain.valueobjects.UserStatus;
import br.dev.andrestamatto.identityhub.domain.valueobjects.UsernameType;

public interface UserRegistrationPolicy {
    UserStatus initialStatusFor(UsernameType usernameType);
}
