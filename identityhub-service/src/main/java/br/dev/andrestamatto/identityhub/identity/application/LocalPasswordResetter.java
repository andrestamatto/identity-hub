package br.dev.andrestamatto.identityhub.identity.application;

import br.dev.andrestamatto.identityhub.identity.domain.LocalPassword;
import br.dev.andrestamatto.identityhub.identity.domain.LoginEmail;
import br.dev.andrestamatto.identityhub.identity.domain.UserAccountRef;

public interface LocalPasswordResetter {

    void reset(UserAccountRef userAccountRef, LoginEmail expectedEmail, LocalPassword password);
}
