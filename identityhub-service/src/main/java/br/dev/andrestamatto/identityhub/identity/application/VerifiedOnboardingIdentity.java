package br.dev.andrestamatto.identityhub.identity.application;

import br.dev.andrestamatto.identityhub.identity.domain.UserAccountRef;
import java.util.Objects;

public record VerifiedOnboardingIdentity(UserAccountRef userAccountRef) {

    public VerifiedOnboardingIdentity {
        Objects.requireNonNull(userAccountRef);
    }
}
