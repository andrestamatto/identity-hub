package br.dev.andrestamatto.identityhub.identity.application;

import br.dev.andrestamatto.identityhub.identity.domain.LoginEmail;
import br.dev.andrestamatto.identityhub.identity.domain.PasswordRecoveryChallenge;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public final class RequestPasswordRecovery {

    private static final Duration TIME_TO_LIVE = Duration.ofMinutes(15);
    private static final Duration REQUEST_WINDOW = Duration.ofMinutes(15);
    private static final int MAXIMUM_REQUESTS = 3;

    private final PasswordRecoveryIdentityFinder identityFinder;
    private final PasswordRecoveryChallengeRepository repository;
    private final RecoveryEmailRequester emailRequester;
    private final IdentityTransaction transaction;
    private final PasswordRecoverySecretGenerator secretGenerator;
    private final Clock clock;
    private final Supplier<UUID> idGenerator;
    private final URI publicBaseUri;

    public RequestPasswordRecovery(
            PasswordRecoveryIdentityFinder identityFinder,
            PasswordRecoveryChallengeRepository repository,
            RecoveryEmailRequester emailRequester,
            IdentityTransaction transaction,
            PasswordRecoverySecretGenerator secretGenerator,
            Clock clock,
            Supplier<UUID> idGenerator,
            URI publicBaseUri) {
        this.identityFinder = Objects.requireNonNull(identityFinder);
        this.repository = Objects.requireNonNull(repository);
        this.emailRequester = Objects.requireNonNull(emailRequester);
        this.transaction = Objects.requireNonNull(transaction);
        this.secretGenerator = Objects.requireNonNull(secretGenerator);
        this.clock = Objects.requireNonNull(clock);
        this.idGenerator = Objects.requireNonNull(idGenerator);
        this.publicBaseUri = requireBaseUri(publicBaseUri);
    }

    public void execute(Command command) {
        Objects.requireNonNull(command);
        var requestedEmail = new LoginEmail(command.email());
        identityFinder.findEligible(requestedEmail).ifPresent(identity -> request(
                command, identity));
    }

    private void request(Command command, PasswordRecoveryIdentity identity) {
        var now = clock.instant();
        var challengeId = idGenerator.get();
        var secret = secretGenerator.generate();
        var digest = PasswordRecoveryDigest.from(secret.value());
        try {
            var challenge = PasswordRecoveryChallenge.start(
                    challengeId,
                    identity.userAccountRef(),
                    command.applicationId(),
                    identity.email(),
                    digest,
                    now,
                    now.plus(TIME_TO_LIVE));
            transaction.execute(() -> {
                repository.replaceActive(
                        challenge, now.minus(REQUEST_WINDOW), MAXIMUM_REQUESTS);
                emailRequester.request(new RecoveryEmailRequester.Command(
                        challengeId,
                        command.applicationId(),
                        identity.email().contactValue(),
                        recoveryUrl(challengeId, secret),
                        command.correlationId()));
            });
        } finally {
            Arrays.fill(digest, (byte) 0);
        }
    }

    private String recoveryUrl(UUID challengeId, PasswordRecoverySecret secret) {
        return publicBaseUri.resolve(
                        "/recover-password#token=" + challengeId + "." + secret.value())
                .toString();
    }

    private static URI requireBaseUri(URI value) {
        Objects.requireNonNull(value);
        if (!value.isAbsolute() || value.getHost() == null
                || value.getUserInfo() != null || value.getQuery() != null
                || value.getFragment() != null
                || !("https".equalsIgnoreCase(value.getScheme())
                        || ("http".equalsIgnoreCase(value.getScheme())
                                && isLoopback(value.getHost())))) {
            throw new IllegalArgumentException("Public base URI is invalid");
        }
        return value;
    }

    private static boolean isLoopback(String host) {
        return "127.0.0.1".equals(host)
                || "localhost".equalsIgnoreCase(host)
                || "::1".equals(host);
    }

    public record Command(
            UUID applicationId,
            String email,
            String correlationId) {

        public Command {
            Objects.requireNonNull(applicationId);
            Objects.requireNonNull(email);
            Objects.requireNonNull(correlationId);
        }

        @Override
        public String toString() {
            return "RequestPasswordRecovery.Command[applicationId=" + applicationId
                    + ", email=REDACTED]";
        }
    }
}
