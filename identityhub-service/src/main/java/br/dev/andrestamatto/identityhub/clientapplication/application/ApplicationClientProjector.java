package br.dev.andrestamatto.identityhub.clientapplication.application;

@FunctionalInterface
public interface ApplicationClientProjector {

    void project(ApplicationClientSnapshot client);
}
