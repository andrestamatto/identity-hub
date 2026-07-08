package br.dev.andrestamatto.identityhub.infrastructure.messaging.template.sms;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.SmsRenderer;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.sms.SmsContent;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.SmsMessageTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * SmsRenderer implementation that resolves an SmsTemplate from the notification
 * message and renders a provider-ready plain-text SMS.
 */
public class TemplatedSmsRenderer implements SmsRenderer {

    private final Map<SmsMessageTemplate, SmsTemplate> smsTemplates;

    public TemplatedSmsRenderer(List<SmsTemplate> smsTemplates) {
        this.smsTemplates = smsTemplates.stream()
                .collect(Collectors.toMap(SmsTemplate::supports, Function.identity()));
    }

    @Override
    public SmsContent render(NotificationMessage notificationMessage) {
        var template = resolveSmsTemplate(notificationMessage);
        var body = template.render(notificationMessage);

        return new SmsContent(notificationMessage.recipient(), body);
    }

    public SmsTemplate resolveSmsTemplate(NotificationMessage notificationMessage) {
        var messageTemplates = notificationMessage.messageTemplates();

        return Optional.ofNullable(smsTemplates.get(messageTemplates.smsMessageTemplate()))
                .orElseThrow(() -> new IllegalArgumentException("SMS template not found: " + messageTemplates.smsMessageTemplate()));
    }
}
