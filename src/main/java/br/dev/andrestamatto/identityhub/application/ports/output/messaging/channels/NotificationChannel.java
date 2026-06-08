package br.dev.andrestamatto.identityhub.application.ports.output.messaging.channels;

/**
 * Logical delivery channel requested for a notification.
 * This is different from the technical provider: EMAIL is a channel, while SMTP/SES
 * are infrastructure choices used to deliver that channel.
 */
public enum NotificationChannel {
    EMAIL,
    SMS,
    WHATSAPP
}
