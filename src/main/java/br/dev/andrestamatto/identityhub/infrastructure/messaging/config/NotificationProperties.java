package br.dev.andrestamatto.identityhub.infrastructure.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="identity-hub.notification")
public record NotificationProperties(
    EmailNotification email
) {

    public record EmailNotification(
        boolean enabled,
        String provider,
        String from,
        Smtp smtp
    ) {}

    public record Smtp(
        String host,
        int port,
        String username,
        String password,
        boolean auth,
        boolean starttls,
        int connectionTimeout,
        int readTimeout,
        int writeTimeout,
        int maxAttempts,
        int retryBackoffMillis
    ){}
}
