package br.dev.andrestamatto.identityhub.clientapplication.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MachineSettingsTest {

    @Test
    void identifiesMachineClientWithoutBrowserOrApiSettings() {
        assertThat(new MachineSettings().type()).isEqualTo(ApplicationClientType.MACHINE);
    }
}
