package br.dev.andrestamatto.identityhub.identity.application;

import br.dev.andrestamatto.identityhub.identity.domain.UserAccountRef;
import java.util.Objects;

public record LocalIdentityRegistration(UserAccountRef userAccountRef, boolean created) {

    public LocalIdentityRegistration {
        Objects.requireNonNull(userAccountRef);
    }
}
