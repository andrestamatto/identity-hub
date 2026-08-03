package br.dev.andrestamatto.identityhub.clientapplication.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class MachineSettingsTest {

    @Test
    void identifiesMachineClientWithoutBrowserOrApiSettings() {
        assertThat(new MachineSettings().type()).isEqualTo(ApplicationClientType.MACHINE);
    }

    @Test
    void acceptsOnlyExplicitUniqueMachineScopes() {
        var settings = new MachineSettings(List.of(MachineClientScope.MEMBERSHIP_WRITE));

        assertThat(settings.scopes()).containsExactly(MachineClientScope.MEMBERSHIP_WRITE);
        assertThat(MachineClientScope.from("membership:write"))
                .isEqualTo(MachineClientScope.MEMBERSHIP_WRITE);
        assertThatThrownBy(() -> MachineClientScope.from("unknown:write"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MachineSettings(List.of(
                        MachineClientScope.MEMBERSHIP_WRITE,
                        MachineClientScope.MEMBERSHIP_WRITE)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
