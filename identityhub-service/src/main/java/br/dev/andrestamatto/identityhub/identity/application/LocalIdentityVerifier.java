package br.dev.andrestamatto.identityhub.identity.application;

import br.dev.andrestamatto.identityhub.identity.domain.UserAccountRef;
import br.dev.andrestamatto.identityhub.identity.domain.LoginEmail;

@FunctionalInterface
public interface LocalIdentityVerifier {

    void verifyAndEnable(UserAccountRef userAccountRef, LoginEmail expectedEmail);
}
