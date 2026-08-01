package br.dev.andrestamatto.identityhub.identity.application;

import br.dev.andrestamatto.identityhub.identity.domain.EmailVerificationChallenge;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationChallengeRepository {

    void replaceActive(
            EmailVerificationChallenge challenge,
            Instant windowStart,
            int maximumRequests);

    Optional<EmailVerificationChallenge> findForUpdate(UUID id);

    void update(EmailVerificationChallenge challenge);
}
