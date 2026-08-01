package br.dev.andrestamatto.identityhub.identity.application;

import br.dev.andrestamatto.identityhub.identity.domain.EmailVerificationDecision;
import java.time.Clock;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public final class ConfirmEmailVerification {

    private final EmailVerificationChallengeRepository repository;
    private final LocalIdentityVerifier verifier;
    private final VerificationTransaction transaction;
    private final Clock clock;

    public ConfirmEmailVerification(
            EmailVerificationChallengeRepository repository,
            LocalIdentityVerifier verifier,
            VerificationTransaction transaction,
            Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.verifier = Objects.requireNonNull(verifier);
        this.transaction = Objects.requireNonNull(transaction);
        this.clock = Objects.requireNonNull(clock);
    }

    public void execute(String token) {
        var parsed = ParsedToken.from(token);
        var digest = EmailVerificationDigest.from(parsed.secret().value());
        var decision = new AtomicReference<>(EmailVerificationDecision.INACTIVE);
        try {
            transaction.execute(() -> repository.findForUpdate(parsed.challengeId())
                    .ifPresent(challenge -> {
                        var current = challenge.validate(digest, clock.instant());
                        decision.set(current);
                        if (current == EmailVerificationDecision.VALID) {
                            verifier.verifyAndEnable(
                                    challenge.userAccountRef(), challenge.email());
                            challenge.markUsed(clock.instant());
                        }
                        if (current != EmailVerificationDecision.INACTIVE) {
                            repository.update(challenge);
                        }
                    }));
        } finally {
            Arrays.fill(digest, (byte) 0);
        }
        if (decision.get() != EmailVerificationDecision.VALID) {
            throw new EmailVerificationRejectedException();
        }
    }

    private record ParsedToken(UUID challengeId, EmailVerificationSecret secret) {

        private static ParsedToken from(String token) {
            Objects.requireNonNull(token);
            var separator = token.indexOf('.');
            if (separator < 1 || separator != token.lastIndexOf('.')
                    || separator == token.length() - 1) {
                throw new EmailVerificationRejectedException();
            }
            try {
                return new ParsedToken(
                        UUID.fromString(token.substring(0, separator)),
                        new EmailVerificationSecret(token.substring(separator + 1)));
            } catch (IllegalArgumentException exception) {
                throw new EmailVerificationRejectedException();
            }
        }
    }
}
