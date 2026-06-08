package br.dev.andrestamatto.identityhub.application.ports.output.messaging.channels;

import java.util.Set;

/**
 * Immutable set of logical channels to be used by one notification message.
 * It lets a use case/listener request delivery through one or more channels
 * without knowing which provider will handle each channel.
 */
public record NotificationChannels(Set<NotificationChannel> values) {

    public NotificationChannels {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("notification channels cannot be null or empty");
        }
        values = Set.copyOf(values);
    }

    public static NotificationChannels email() {
        return new NotificationChannels(Set.of(NotificationChannel.EMAIL));
    }

    public static NotificationChannels sms() {
        return new NotificationChannels(Set.of(NotificationChannel.SMS));
    }

    public static NotificationChannels emailAndSms() {
        return new NotificationChannels(Set.of(NotificationChannel.EMAIL, NotificationChannel.SMS));
    }
}
