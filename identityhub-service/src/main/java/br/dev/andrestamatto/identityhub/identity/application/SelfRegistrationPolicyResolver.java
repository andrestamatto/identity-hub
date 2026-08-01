package br.dev.andrestamatto.identityhub.identity.application;

import java.util.UUID;

@FunctionalInterface
public interface SelfRegistrationPolicyResolver {

    boolean isEnabled(UUID applicationId);
}
