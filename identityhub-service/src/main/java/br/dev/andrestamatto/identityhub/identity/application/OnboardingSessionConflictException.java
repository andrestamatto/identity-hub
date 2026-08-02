package br.dev.andrestamatto.identityhub.identity.application;

public final class OnboardingSessionConflictException extends RuntimeException {

    public OnboardingSessionConflictException() {
        super("Idempotency key is already bound to different onboarding content");
    }
}
