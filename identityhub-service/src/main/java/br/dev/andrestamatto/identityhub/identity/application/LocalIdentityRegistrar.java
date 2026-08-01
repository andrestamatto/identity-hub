package br.dev.andrestamatto.identityhub.identity.application;

@FunctionalInterface
public interface LocalIdentityRegistrar {

    LocalIdentityRegistration register(PendingLocalIdentity identity);
}
