package br.dev.andrestamatto.identityhub.identity.application;

@FunctionalInterface
public interface OnboardingSessionIdGenerator {

    String generate();
}
