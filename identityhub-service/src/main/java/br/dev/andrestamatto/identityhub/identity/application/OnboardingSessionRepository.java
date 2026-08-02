package br.dev.andrestamatto.identityhub.identity.application;

import br.dev.andrestamatto.identityhub.identity.domain.OnboardingSession;

public interface OnboardingSessionRepository {

    SaveResult saveOrFind(OnboardingSession session);

    record SaveResult(OnboardingSession session, boolean created) {
    }
}
