package br.dev.andrestamatto.identityhub.identity.application;

import br.dev.andrestamatto.identityhub.identity.domain.LoginEmail;
import java.util.Optional;

public interface PasswordRecoveryIdentityFinder {

    Optional<PasswordRecoveryIdentity> findEligible(LoginEmail email);
}
