package br.dev.andrestamatto.identityhub.clientapplication.domain;

public sealed interface ApplicationClientSettings
        permits BffSettings, ProtectedApiSettings, SpaSettings {

    ApplicationClientType type();
}
