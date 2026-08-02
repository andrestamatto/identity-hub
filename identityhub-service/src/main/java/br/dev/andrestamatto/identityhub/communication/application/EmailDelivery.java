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
        String sensitiveContent,
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
        validateSensitiveContent(purpose, state, sensitiveContent);
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
                null,
                EmailDeliveryState.PENDING,
                0,
                now,
                null,
                requireCorrelationId(correlationId),
                now,
                now);
    }

    public static EmailDelivery requestVerification(
            EmailDeliveryId id,
            EmailOrigin origin,
            EmailRecipient recipient,
            String verificationUrl,
            String correlationId,
            Instant now) {
        return new EmailDelivery(
                id,
                origin.applicationId(),
                origin.applicationIdentifier(),
                origin.applicationDisplayName(),
                origin.environment(),
                recipient,
                EmailDeliveryPurpose.EMAIL_VERIFICATION,
                requireSensitiveContent(verificationUrl),
                EmailDeliveryState.PENDING,
                0,
                now,
                null,
                requireCorrelationId(correlationId),
                now,
                now);
    }

    public static EmailDelivery requestPasswordRecovery(
            EmailDeliveryId id,
            EmailOrigin origin,
            EmailRecipient recipient,
            String recoveryUrl,
            String correlationId,
            Instant now) {
        return new EmailDelivery(
                id,
                origin.applicationId(),
                origin.applicationIdentifier(),
                origin.applicationDisplayName(),
                origin.environment(),
                recipient,
                EmailDeliveryPurpose.PASSWORD_RECOVERY,
                requireSensitiveContent(recoveryUrl),
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
            String sensitiveContent,
            EmailDeliveryState state,
            int attempts,
            Instant nextAttemptAt,
            String lastFailureCode,
            String correlationId,
            Instant requestedAt,
            Instant updatedAt) {
        return new EmailDelivery(
                id, applicationId, applicationIdentifier, applicationDisplayName, environment,
                recipient, purpose, sensitiveContent, state, attempts, nextAttemptAt, lastFailureCode,
                correlationId, requestedAt, updatedAt);
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
        return reconstitute(
                id, applicationId, applicationIdentifier, applicationDisplayName,
                environment, recipient, purpose, null, state, attempts, nextAttemptAt,
                lastFailureCode, correlationId, requestedAt, updatedAt);
    }

    public boolean matchesVerification(
            UUID expectedApplicationId,
            EmailRecipient expectedRecipient,
            String expectedCorrelationId) {
        return applicationId.equals(expectedApplicationId)
                && recipient.equals(expectedRecipient)
                && correlationId.equals(requireCorrelationId(expectedCorrelationId))
                && purpose == EmailDeliveryPurpose.EMAIL_VERIFICATION;
    }

    public boolean matchesPasswordRecovery(
            UUID expectedApplicationId,
            EmailRecipient expectedRecipient,
            String expectedCorrelationId) {
        return applicationId.equals(expectedApplicationId)
                && recipient.equals(expectedRecipient)
                && correlationId.equals(requireCorrelationId(expectedCorrelationId))
                && purpose == EmailDeliveryPurpose.PASSWORD_RECOVERY;
    }

    @Override
    public String toString() {
        return "EmailDelivery[id=" + id + ", purpose=" + purpose + ", state=" + state
                + ", sensitiveContent=REDACTED]";
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

    private static String requireSensitiveContent(String value) {
        Objects.requireNonNull(value);
        if (value.isBlank() || value.length() > 2048
                || value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Sensitive email content is invalid");
        }
        return value;
    }

    private static void validateSensitiveContent(
            EmailDeliveryPurpose purpose,
            EmailDeliveryState state,
            String sensitiveContent) {
        if (purpose == EmailDeliveryPurpose.PASSWORD_CHANGED && sensitiveContent != null) {
            throw new IllegalArgumentException("Password email cannot contain sensitive content");
        }
        if (hasSensitiveLink(purpose)
                && state == EmailDeliveryState.PENDING) {
            requireSensitiveContent(sensitiveContent);
        }
        if (hasSensitiveLink(purpose)
                && state != EmailDeliveryState.PENDING
                && sensitiveContent != null) {
            throw new IllegalArgumentException("Completed email cannot retain sensitive content");
        }
    }

    private static boolean hasSensitiveLink(EmailDeliveryPurpose purpose) {
        return purpose == EmailDeliveryPurpose.EMAIL_VERIFICATION
                || purpose == EmailDeliveryPurpose.PASSWORD_RECOVERY;
    }
}
