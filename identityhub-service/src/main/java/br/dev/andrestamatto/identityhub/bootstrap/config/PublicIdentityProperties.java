package br.dev.andrestamatto.identityhub.bootstrap.config;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("identityhub.public-identity")
public record PublicIdentityProperties(
        boolean enabled,
        int registrationRequestLimit,
        Duration registrationWindow,
        int trackedSourceLimit,
        Duration minimumResponseTime,
        int maximumRequestBytes) {

    private static final Duration MAXIMUM_RESPONSE_TIME = Duration.ofSeconds(5);

    public PublicIdentityProperties {
        Objects.requireNonNull(registrationWindow);
        Objects.requireNonNull(minimumResponseTime);
        if (registrationRequestLimit < 1
                || registrationWindow.isZero()
                || registrationWindow.isNegative()
                || trackedSourceLimit < 1
                || minimumResponseTime.isZero()
                || minimumResponseTime.isNegative()
                || minimumResponseTime.compareTo(MAXIMUM_RESPONSE_TIME) > 0
                || maximumRequestBytes < 512
                || maximumRequestBytes > 16_384) {
            throw new IllegalArgumentException(
                    "Public identity protection settings are outside safe bounds");
        }
    }
}
