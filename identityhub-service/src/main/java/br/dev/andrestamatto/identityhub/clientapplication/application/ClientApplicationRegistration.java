package br.dev.andrestamatto.identityhub.clientapplication.application;

public record ClientApplicationRegistration(
        ClientApplicationSnapshot application,
        boolean created) {
}
