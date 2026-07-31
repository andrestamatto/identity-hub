package br.dev.andrestamatto.identityhub.clientapplication.application;

public interface BffClientSecretRotator {

    ConfidentialClientSecret rotate(ApplicationClientSnapshot client);
}
