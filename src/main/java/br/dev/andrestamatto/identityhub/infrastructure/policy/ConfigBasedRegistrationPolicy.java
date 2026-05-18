package br.dev.andrestamatto.identityhub.infrastructure.policy;

import br.dev.andrestamatto.identityhub.application.ports.input.UserRegistrationPolicy;
import br.dev.andrestamatto.identityhub.domain.valueobjects.UserStatus;
import br.dev.andrestamatto.identityhub.domain.valueobjects.UsernameType;

public final class ConfigBasedRegistrationPolicy implements UserRegistrationPolicy {

    private final PoliciesProperties policiesProperties;

    public ConfigBasedRegistrationPolicy(PoliciesProperties policiesProperties) {
        this.policiesProperties = policiesProperties;
    }

    @Override
    public UserStatus initialStatusFor(UsernameType usernameType) {

        if (usernameType == null) { throw new IllegalArgumentException("UsernameType cannot be null"); }

        return (
                policiesProperties.usernameTypePolicies().enableVerificationUponRegistration()
                ? UserStatus.PENDING_VERIFICATION
                : UserStatus.ACTIVE
        );

    }
}
