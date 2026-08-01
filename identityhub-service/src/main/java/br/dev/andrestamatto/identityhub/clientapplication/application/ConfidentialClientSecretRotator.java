package br.dev.andrestamatto.identityhub.clientapplication.application;

public interface ConfidentialClientSecretRotator {

    ConfidentialClientSecret rotate(ApplicationClientSnapshot client);
}
