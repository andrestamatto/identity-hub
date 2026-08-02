package br.dev.andrestamatto.identityhub.identity.application;

import br.dev.andrestamatto.identityhub.identity.domain.PasswordRecoveryChallenge;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordRecoveryChallengeRepository {

    void replaceActive(
            PasswordRecoveryChallenge challenge,
            Instant windowStart,
            int maximumRequests);

    Optional<PasswordRecoveryChallenge> findForUpdate(UUID id);

    void update(PasswordRecoveryChallenge challenge);
}
