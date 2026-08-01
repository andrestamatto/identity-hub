package br.dev.andrestamatto.identityhub.identity.application;

import br.dev.andrestamatto.identityhub.identity.domain.LocalPassword;
import br.dev.andrestamatto.identityhub.identity.domain.LoginEmail;
import java.util.Objects;

public record PendingLocalIdentity(LoginEmail email, LocalPassword password) {

    public PendingLocalIdentity {
        Objects.requireNonNull(email);
        Objects.requireNonNull(password);
    }
}
