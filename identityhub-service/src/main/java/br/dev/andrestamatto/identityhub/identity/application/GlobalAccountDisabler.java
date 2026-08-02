package br.dev.andrestamatto.identityhub.identity.application;

import br.dev.andrestamatto.identityhub.identity.domain.UserAccountRef;

public interface GlobalAccountDisabler {

    void disable(UserAccountRef userAccountRef);
}
