package br.dev.andrestamatto.identityhub.identity.application;

import br.dev.andrestamatto.identityhub.identity.domain.OnboardingDigest;
import br.dev.andrestamatto.identityhub.identity.domain.OnboardingSessionId;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class IssueOnboardingIdentityProof {

    private final OnboardingSessionRepository repository;
    private final OnboardingProofTransaction transaction;
    private final Clock clock;
    private final OnboardingProofTokenGenerator tokenGenerator;

    public IssueOnboardingIdentityProof(
            OnboardingSessionRepository repository,
            OnboardingProofTransaction transaction,
            Clock clock,
            OnboardingProofTokenGenerator tokenGenerator) {
        this.repository = Objects.requireNonNull(repository);
        this.transaction = Objects.requireNonNull(transaction);
        this.clock = Objects.requireNonNull(clock);
        this.tokenGenerator = Objects.requireNonNull(tokenGenerator);
    }

    public Result execute(String sessionId, VerifiedOnboardingIdentity identity) {
        Objects.requireNonNull(identity);
        final OnboardingSessionId parsedSessionId;
        try {
            parsedSessionId = new OnboardingSessionId(sessionId);
        } catch (RuntimeException exception) {
            throw new OnboardingProofRejectedException();
        }
        var result = new AtomicReference<Result>();
        transaction.execute(() -> {
            var session = repository.findForUpdate(parsedSessionId)
                    .orElseThrow(OnboardingProofRejectedException::new);
            try {
                var now = clock.instant();
                var rawProof = tokenGenerator.generate();
                if (rawProof == null || !rawProof.matches("[A-Za-z0-9_-]{43}")) {
                    throw new IllegalStateException("Generated onboarding proof is invalid");
                }
                var issuance = session.issueProof(
                        digest(rawProof), identity.userAccountRef(), now);
                repository.saveIssuedProof(issuance);
                result.set(new Result(rawProof, issuance.proof().expiresAt()));
            } catch (IllegalStateException exception) {
                throw new OnboardingProofRejectedException();
            }
        });
        return result.get();
    }

    private static OnboardingDigest digest(String value) {
        Objects.requireNonNull(value);
        try {
            var bytes = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.US_ASCII));
            return new OnboardingDigest(HexFormat.of().formatHex(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record Result(String proof, Instant expiresAt) {

        public Result {
            Objects.requireNonNull(proof);
            Objects.requireNonNull(expiresAt);
        }

        @Override
        public String toString() {
            return "Result[proof=[REDACTED], expiresAt=" + expiresAt + "]";
        }
    }
}
