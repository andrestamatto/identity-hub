package br.dev.andrestamatto.identityhub.identity.application;

import java.util.UUID;

@FunctionalInterface
public interface OnboardingOriginResolver {

    UUID resolve(UUID machineClientId, UUID browserClientId, String redirectUri);
}
