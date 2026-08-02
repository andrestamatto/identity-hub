package br.dev.andrestamatto.identityhub.clientapplication.domain;

public record MachineSettings() implements ApplicationClientSettings {

    @Override
    public ApplicationClientType type() {
        return ApplicationClientType.MACHINE;
    }
}
