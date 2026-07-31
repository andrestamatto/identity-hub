package br.dev.andrestamatto.identityhub.clientapplication.application;

public record ApplicationClientConfigurationResult(
        ApplicationClientSnapshot client,
        boolean created) {
}
