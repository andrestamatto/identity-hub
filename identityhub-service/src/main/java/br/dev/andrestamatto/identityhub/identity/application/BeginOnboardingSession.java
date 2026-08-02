package br.dev.andrestamatto.identityhub.identity.application;

import br.dev.andrestamatto.identityhub.identity.domain.OnboardingDigest;
import br.dev.andrestamatto.identityhub.identity.domain.OnboardingSession;
import br.dev.andrestamatto.identityhub.identity.domain.OnboardingSessionId;
import br.dev.andrestamatto.identityhub.identity.domain.PkceCodeChallenge;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public final class BeginOnboardingSession {

    private static final Pattern ACQUISITION_REFERENCE =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");
    private static final Pattern IDEMPOTENCY_KEY =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{15,127}");
    private static final Pattern CORRELATION_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    private final OnboardingOriginResolver originResolver;
    private final OnboardingSessionRepository repository;
    private final Clock clock;
    private final OnboardingSessionIdGenerator idGenerator;

    public BeginOnboardingSession(
            OnboardingOriginResolver originResolver,
            OnboardingSessionRepository repository,
            Clock clock,
            OnboardingSessionIdGenerator idGenerator) {
        this.originResolver = Objects.requireNonNull(originResolver);
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
        this.idGenerator = Objects.requireNonNull(idGenerator);
    }

    public Result execute(Command command) {
        Objects.requireNonNull(command);
        requireMatches(command.acquisitionReference(), ACQUISITION_REFERENCE,
                "Invalid acquisition reference");
        requireMatches(command.idempotencyKey(), IDEMPOTENCY_KEY,
                "Invalid idempotency key");
        requireMatches(command.correlationId(), CORRELATION_ID,
                "Invalid correlation id");
        var codeChallenge = new PkceCodeChallenge(command.codeChallenge());
        var applicationId = originResolver.resolve(
                command.machineClientId(), command.browserClientId(), command.redirectUri());
        var acquisitionDigest = digest(command.acquisitionReference());
        var idempotencyDigest = digest(command.idempotencyKey());
        var requestDigest = digest(String.join(
                "\u001f",
                applicationId.toString(),
                command.machineClientId().toString(),
                command.browserClientId().toString(),
                acquisitionDigest.value(),
                command.redirectUri(),
                codeChallenge.value()));
        var candidate = OnboardingSession.initiate(
                new OnboardingSessionId(idGenerator.generate()),
                applicationId,
                command.machineClientId(),
                command.browserClientId(),
                acquisitionDigest,
                command.redirectUri(),
                codeChallenge,
                idempotencyDigest,
                requestDigest,
                command.correlationId(),
                clock.instant());
        var persisted = repository.saveOrFind(candidate);
        if (!persisted.session().requestDigest().equals(candidate.requestDigest())) {
            throw new OnboardingSessionConflictException();
        }
        return new Result(
                persisted.session().id().value(),
                persisted.session().expiresAt(),
                persisted.created());
    }

    private static OnboardingDigest digest(String value) {
        try {
            var bytes = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return new OnboardingDigest(HexFormat.of().formatHex(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void requireMatches(String value, Pattern pattern, String message) {
        Objects.requireNonNull(value);
        if (!pattern.matcher(value).matches()) {
            throw new IllegalArgumentException(message);
        }
    }

    public record Command(
            UUID machineClientId,
            UUID browserClientId,
            String acquisitionReference,
            String redirectUri,
            String codeChallenge,
            String idempotencyKey,
            String correlationId) {

        public Command {
            Objects.requireNonNull(machineClientId);
            Objects.requireNonNull(browserClientId);
            Objects.requireNonNull(redirectUri);
        }
    }

    public record Result(String sessionId, Instant expiresAt, boolean created) {
    }
}
