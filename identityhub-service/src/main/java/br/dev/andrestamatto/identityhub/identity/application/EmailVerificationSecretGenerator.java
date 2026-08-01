package br.dev.andrestamatto.identityhub.identity.application;

@FunctionalInterface
public interface EmailVerificationSecretGenerator {

    EmailVerificationSecret generate();
}
