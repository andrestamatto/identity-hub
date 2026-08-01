package br.dev.andrestamatto.identityhub.bootstrap.config;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("identityhub.communication.email")
public record EmailDeliveryProperties(
        boolean enabled,
        String fromAddress,
        Duration pollInterval,
        Duration leaseDuration,
        Duration initialRetryDelay,
        int maxAttempts) {

    public EmailDeliveryProperties {
        if (enabled) {
            requireText(fromAddress, "identityhub.communication.email.from-address");
            requirePositive(pollInterval, "identityhub.communication.email.poll-interval");
            requirePositive(leaseDuration, "identityhub.communication.email.lease-duration");
            requirePositive(
                    initialRetryDelay,
                    "identityhub.communication.email.initial-retry-delay");
            if (maxAttempts < 1) {
                throw new IllegalArgumentException(
                        "identityhub.communication.email.max-attempts must be positive");
            }
        }
    }

    private static void requireText(String value, String name) {
        Objects.requireNonNull(value, name + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
    }

    private static void requirePositive(Duration value, String name) {
        Objects.requireNonNull(value, name + " is required");
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
