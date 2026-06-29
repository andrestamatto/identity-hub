package br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates;

/**
 * Template selection for each supported channel in a notification.
 * A message can carry different template identifiers for email, SMS, and WhatsApp.
 */
public record MessageTemplates(
        EmailMessageTemplate emailMessageTemplate,
        SmsMessageTemplate smsMessageTemplate,
        WhatsappMessageTemplate whatsappMessageTemplate
) {
    public MessageTemplates(EmailMessageTemplate emailMessageTemplate, SmsMessageTemplate smsMessageTemplate) {
        this(emailMessageTemplate, smsMessageTemplate, WhatsappMessageTemplate.UNDEFINED);
    }
}
