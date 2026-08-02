package br.dev.andrestamatto.identityhub.identity.application;

import br.dev.andrestamatto.identityhub.identity.domain.EmailVerificationChallenge;
import br.dev.andrestamatto.identityhub.identity.domain.LoginEmail;
import br.dev.andrestamatto.identityhub.identity.domain.UserAccountRef;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public final class RequestEmailVerification {

    private static final Duration TIME_TO_LIVE = Duration.ofMinutes(30);
    private static final Duration REQUEST_WINDOW = Duration.ofMinutes(15);
    private static final int MAXIMUM_REQUESTS = 3;

    private final EmailVerificationChallengeRepository repository;
    private final VerificationEmailRequester emailRequester;
    private final IdentityTransaction transaction;
    private final EmailVerificationSecretGenerator secretGenerator;
    private final Clock clock;
    private final Supplier<UUID> idGenerator;
    private final URI publicBaseUri;

    public RequestEmailVerification(
            EmailVerificationChallengeRepository repository,
            VerificationEmailRequester emailRequester,
            IdentityTransaction transaction,
            EmailVerificationSecretGenerator secretGenerator,
            Clock clock,
            Supplier<UUID> idGenerator,
            URI publicBaseUri) {
        this.repository = Objects.requireNonNull(repository);
        this.emailRequester = Objects.requireNonNull(emailRequester);
        this.transaction = Objects.requireNonNull(transaction);
        this.secretGenerator = Objects.requireNonNull(secretGenerator);
        this.clock = Objects.requireNonNull(clock);
        this.idGenerator = Objects.requireNonNull(idGenerator);
        this.publicBaseUri = requireBaseUri(publicBaseUri);
    }

    public Result execute(Command command) {
        Objects.requireNonNull(command);
        var now = clock.instant();
        var challengeId = idGenerator.get();
        var email = new LoginEmail(command.recipient());
        var secret = secretGenerator.generate();
        var digest = EmailVerificationDigest.from(secret.value());
        try {
            var challenge = EmailVerificationChallenge.start(
                    challengeId,
                    new UserAccountRef(command.userAccountRef()),
                    command.applicationId(),
                    email,
                    digest,
                    now,
                    now.plus(TIME_TO_LIVE));
            transaction.execute(() -> {
                repository.replaceActive(
                        challenge, now.minus(REQUEST_WINDOW), MAXIMUM_REQUESTS);
                emailRequester.request(new VerificationEmailRequester.Command(
                        challengeId,
                        command.applicationId(),
                        email.contactValue(),
                        verificationUrl(challengeId, secret),
                        command.correlationId()));
            });
            return new Result(challengeId);
        } finally {
            Arrays.fill(digest, (byte) 0);
        }
    }

    private String verificationUrl(UUID challengeId, EmailVerificationSecret secret) {
        return publicBaseUri.resolve("/verify-email#token=" + challengeId + "." + secret.value())
                .toString();
    }

    private static URI requireBaseUri(URI value) {
        Objects.requireNonNull(value);
        if (!value.isAbsolute() || value.getHost() == null
                || value.getUserInfo() != null
                || value.getQuery() != null || value.getFragment() != null
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
            UUID userAccountRef,
            String recipient,
            String correlationId) {

        public Command {
            Objects.requireNonNull(applicationId);
            Objects.requireNonNull(userAccountRef);
            Objects.requireNonNull(recipient);
            Objects.requireNonNull(correlationId);
        }

        @Override
        public String toString() {
            return "RequestEmailVerification.Command[applicationId=" + applicationId
                    + ", recipient=REDACTED]";
        }
    }

    public record Result(UUID challengeId) { }
}
