package br.dev.andrestamatto.identityhub.identity.application;

@FunctionalInterface
public interface OnboardingProofTransaction {

    void execute(Runnable work);
}
