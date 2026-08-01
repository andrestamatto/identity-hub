package br.dev.andrestamatto.identityhub.communication.application;

import br.dev.andrestamatto.identityhub.communication.domain.EmailDeliveryId;
import br.dev.andrestamatto.identityhub.communication.domain.EmailRecipient;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record EmailDelivery(
        EmailDeliveryId id,
        UUID applicationId,
        String applicationIdentifier,
        String applicationDisplayName,
        String environment,
        EmailRecipient recipient,
        EmailDeliveryPurpose purpose,
        EmailDeliveryState state,
        int attempts,
        Instant nextAttemptAt,
        String lastFailureCode,
        String correlationId,
        Instant requestedAt,
        Instant updatedAt) {

    public EmailDelivery {
        Objects.requireNonNull(id);
        Objects.requireNonNull(applicationId);
        Objects.requireNonNull(applicationIdentifier);
        Objects.requireNonNull(applicationDisplayName);
        Objects.requireNonNull(environment);
        Objects.requireNonNull(recipient);
        Objects.requireNonNull(purpose);
        Objects.requireNonNull(state);
        Objects.requireNonNull(nextAttemptAt);
        Objects.requireNonNull(correlationId);
        Objects.requireNonNull(requestedAt);
        Objects.requireNonNull(updatedAt);
        if (attempts < 0) {
            throw new IllegalArgumentException("Attempts cannot be negative");
        }
    }

    public static EmailDelivery request(
            EmailDeliveryId id,
            EmailOrigin origin,
            EmailRecipient recipient,
            EmailDeliveryPurpose purpose,
            String correlationId,
            Instant now) {
        return new EmailDelivery(
                id,
                origin.applicationId(),
                origin.applicationIdentifier(),
                origin.applicationDisplayName(),
                origin.environment(),
                recipient,
                purpose,
                EmailDeliveryState.PENDING,
                0,
                now,
                null,
                requireCorrelationId(correlationId),
                now,
                now);
    }

    public static EmailDelivery reconstitute(
            EmailDeliveryId id,
            UUID applicationId,
            String applicationIdentifier,
            String applicationDisplayName,
            String environment,
            EmailRecipient recipient,
            EmailDeliveryPurpose purpose,
            EmailDeliveryState state,
            int attempts,
            Instant nextAttemptAt,
            String lastFailureCode,
            String correlationId,
            Instant requestedAt,
            Instant updatedAt) {
        return new EmailDelivery(
                id, applicationId, applicationIdentifier, applicationDisplayName, environment,
                recipient, purpose, state, attempts, nextAttemptAt, lastFailureCode,
                correlationId, requestedAt, updatedAt);
    }

    public boolean matches(UUID expectedApplicationId, EmailRecipient expectedRecipient, String expectedCorrelationId) {
        return applicationId.equals(expectedApplicationId)
                && recipient.equals(expectedRecipient)
                && correlationId.equals(requireCorrelationId(expectedCorrelationId))
                && purpose == EmailDeliveryPurpose.PASSWORD_CHANGED;
    }

    private static String requireCorrelationId(String value) {
        Objects.requireNonNull(value);
        var normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > 128
                || normalized.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Correlation id is invalid");
        }
        return normalized;
    }
}
