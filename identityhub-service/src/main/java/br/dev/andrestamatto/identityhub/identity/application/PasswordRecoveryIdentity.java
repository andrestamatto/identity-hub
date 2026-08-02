package br.dev.andrestamatto.identityhub.identity.application;

import br.dev.andrestamatto.identityhub.identity.domain.LoginEmail;
import br.dev.andrestamatto.identityhub.identity.domain.UserAccountRef;
import java.util.Objects;

public record PasswordRecoveryIdentity(UserAccountRef userAccountRef, LoginEmail email) {

    public PasswordRecoveryIdentity {
        Objects.requireNonNull(userAccountRef);
        Objects.requireNonNull(email);
    }
}
