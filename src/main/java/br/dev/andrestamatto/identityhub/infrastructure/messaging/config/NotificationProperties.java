package br.dev.andrestamatto.identityhub.infrastructure.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="identity-hub.notification")
public record NotificationProperties(
    EmailNotification email,
    SmsNotification sms,
    WhatsAppNotification whatsapp
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

    public record SmsNotification(
            boolean enabled,
            String provider,
            Providers providers
    ) {}

    public record WhatsAppNotification(
            boolean enabled,
            String apiUrl
    ){}

    public record Providers(
            String log,
            TwilioProvider twilio
    ){}

    public record TwilioProvider(
            String accountSid,
            String authToken,
            String from
    ) {}


}
