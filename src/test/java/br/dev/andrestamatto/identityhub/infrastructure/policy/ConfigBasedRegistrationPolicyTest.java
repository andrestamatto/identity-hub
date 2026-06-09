package br.dev.andrestamatto.identityhub.infrastructure.policy;

import br.dev.andrestamatto.identityhub.domain.valueobjects.UserStatus;
import br.dev.andrestamatto.identityhub.domain.valueobjects.UsernameType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ConfigBasedRegistrationPolicyTest {

    @Test
    public void shouldReturnPendingVerificationWhenRegistrationVerificationIsEnabled() {
        var policy = new ConfigBasedRegistrationPolicy(
                new UserRegistrationPolicies(
                        new UserRegistrationPolicies.UsernameTypePolicy(true)
                )
        );

        assertEquals(UserStatus.PENDING_VERIFICATION, policy.initialStatusFor(UsernameType.EMAIL));
    }

    @Test
    public void shouldReturnActiveWhenRegistrationVerificationIsDisabled() {
        var policy = new ConfigBasedRegistrationPolicy(
                new UserRegistrationPolicies(
                        new UserRegistrationPolicies.UsernameTypePolicy(false)
                )
        );

        assertEquals(UserStatus.ACTIVE, policy.initialStatusFor(UsernameType.EMAIL));
    }

    @Test
    public void shouldRejectNullUsernameType() {
        var policy = new ConfigBasedRegistrationPolicy(
                new UserRegistrationPolicies(
                        new UserRegistrationPolicies.UsernameTypePolicy(true)
                )
        );

        assertThrows(IllegalArgumentException.class, () -> policy.initialStatusFor(null));
    }
}
