package br.dev.andrestamatto.identityhub.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.dev.andrestamatto.identityhub.identity.domain.LoginEmail;
import br.dev.andrestamatto.identityhub.identity.domain.UserAccountRef;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RegisterPendingLocalIdentityTest {

    private static final UUID APPLICATION_ID =
            UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0");
    private static final UserAccountRef USER_REF = new UserAccountRef(
            UUID.fromString("9bc4a8c9-405b-4f4a-b443-3c2012369264"));

    @Test
    void registersPendingIdentityOnlyWhenApplicationAllowsIt() {
        var registrar = new RecordingRegistrar();
        var useCase = new RegisterPendingLocalIdentity(
                id -> true,
                registrar);

        var result = useCase.execute(new RegisterPendingLocalIdentity.Command(
                APPLICATION_ID,
                "Andre@Example.com",
                "frase longa com café seguro".toCharArray()));

        assertThat(result.userAccountRef()).isEqualTo(USER_REF.value());
        assertThat(result.created()).isTrue();
        assertThat(registrar.email.normalizedValue()).isEqualTo("andre@example.com");
    }

    @Test
    void deniesBeforeCallingEngineWhenRegistrationIsDisabled() {
        var registrar = new RecordingRegistrar();
        var useCase = new RegisterPendingLocalIdentity(id -> false, registrar);

        assertThatThrownBy(() -> useCase.execute(new RegisterPendingLocalIdentity.Command(
                        APPLICATION_ID,
                        "andre@example.com",
                        "frase longa com café seguro".toCharArray())))
                .isInstanceOf(SelfRegistrationDisabledException.class);
        assertThat(registrar.calls).isZero();
    }

    private static final class RecordingRegistrar implements LocalIdentityRegistrar {
        private int calls;
        private LoginEmail email;

        @Override
        public LocalIdentityRegistration register(PendingLocalIdentity identity) {
            calls++;
            email = identity.email();
            return new LocalIdentityRegistration(USER_REF, true);
        }
    }
}
