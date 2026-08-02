package br.dev.andrestamatto.identityhub.identity.application;

@FunctionalInterface
public interface OnboardingProofTokenGenerator {

    String generate();
}
