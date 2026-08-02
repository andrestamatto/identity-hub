package br.dev.andrestamatto.identityhub.identity.application;

import br.dev.andrestamatto.identityhub.identity.domain.OnboardingSession;
import br.dev.andrestamatto.identityhub.identity.domain.OnboardingProofIssuance;
import br.dev.andrestamatto.identityhub.identity.domain.OnboardingSessionId;
import java.util.Optional;

public interface OnboardingSessionRepository {

    SaveResult saveOrFind(OnboardingSession session);

    Optional<OnboardingSession> findForUpdate(OnboardingSessionId sessionId);

    void saveIssuedProof(OnboardingProofIssuance issuance);

    record SaveResult(OnboardingSession session, boolean created) {
    }
}
