package br.dev.andrestamatto.identityhub.clientapplication.domain;

public sealed interface ApplicationClientSettings
        permits BffSettings, MachineSettings, ProtectedApiSettings, SpaSettings {

    ApplicationClientType type();
}
