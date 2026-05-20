package br.dev.andrestamatto.identityhub.application.ports.output;

import br.dev.andrestamatto.identityhub.domain.valueobjects.UserStatus;
import br.dev.andrestamatto.identityhub.domain.valueobjects.UsernameType;

public interface UserRegistrationPolicy {
    UserStatus initialStatusFor(UsernameType usernameType);
}
