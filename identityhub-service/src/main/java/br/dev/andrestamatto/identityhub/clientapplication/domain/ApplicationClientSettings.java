package br.dev.andrestamatto.identityhub.clientapplication.domain;

public sealed interface ApplicationClientSettings
        permits ProtectedApiSettings, SpaSettings {

    ApplicationClientType type();
}
