package br.dev.andrestamatto.identityhub.identity.application;

import br.dev.andrestamatto.identityhub.identity.domain.PasswordRecoveryChallenge;
import java.time.Instant;

public interface PasswordRecoveryChallengeRepository {

    void replaceActive(
            PasswordRecoveryChallenge challenge,
            Instant windowStart,
            int maximumRequests);
}
