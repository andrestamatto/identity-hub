package br.dev.andrestamatto.identityhub.identity.application;

public final class OnboardingProofRejectedException extends RuntimeException {

    public OnboardingProofRejectedException() {
        super("Onboarding proof cannot be issued");
    }
}
