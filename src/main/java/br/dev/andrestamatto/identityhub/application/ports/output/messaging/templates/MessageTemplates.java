package br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates;

/**
 * Template selection for each supported channel in a notification.
 * A message can carry different template identifiers for email and SMS.
 */
public record MessageTemplates(
        EmailMessageTemplate emailMessageTemplate,
        SmsMessageTemplate smsMessageTemplate
) {
}
