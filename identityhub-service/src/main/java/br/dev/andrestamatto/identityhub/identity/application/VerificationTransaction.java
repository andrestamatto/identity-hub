package br.dev.andrestamatto.identityhub.identity.application;

@FunctionalInterface
public interface VerificationTransaction {

    void execute(Runnable work);
}
