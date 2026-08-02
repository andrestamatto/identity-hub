package br.dev.andrestamatto.identityhub.clientapplication.application;

public final class OnboardingOriginRejectedException extends RuntimeException {

    public OnboardingOriginRejectedException() {
        super("Onboarding origin is not authorized");
    }
}
