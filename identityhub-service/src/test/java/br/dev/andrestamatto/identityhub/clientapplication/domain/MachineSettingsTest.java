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
    void exposesOnlyExplicitSupportedScopes() {
        var settings = MachineSettings.create(List.of("onboarding:write"));

        assertThat(settings.scopes()).containsExactly(MachineScope.ONBOARDING_WRITE);
        assertThat(settings.scopeValues()).containsExactly("onboarding:write");
    }

    @Test
    void rejectsUnknownOrDuplicateScopes() {
        assertThatThrownBy(() -> MachineSettings.create(List.of("payments:write")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scope");
        assertThatThrownBy(() -> MachineSettings.create(List.of(
                        "onboarding:write", "onboarding:write")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate");
    }
}
