package br.dev.andrestamatto.identityhub.infrastructure.policy;

import br.dev.andrestamatto.identityhub.application.ports.output.UserRegistrationPolicy;
import br.dev.andrestamatto.identityhub.domain.valueobjects.UserStatus;
import br.dev.andrestamatto.identityhub.domain.valueobjects.UsernameType;

public final class ConfigBasedRegistrationPolicy implements UserRegistrationPolicy {

    private final UserRegistrationPolicies userRegistrationPolicies;

    public ConfigBasedRegistrationPolicy(UserRegistrationPolicies userRegistrationPolicies) {
        this.userRegistrationPolicies = userRegistrationPolicies;
    }

    @Override
    public UserStatus initialStatusFor(UsernameType usernameType) {

        if (usernameType == null) { throw new IllegalArgumentException("UsernameType cannot be null"); }

        return (
                userRegistrationPolicies.usernameTypePolicy().enableVerificationUponRegistration()
                ? UserStatus.PENDING_VERIFICATION
                : UserStatus.ACTIVE
        );

    }
}
