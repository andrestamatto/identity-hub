package br.dev.andrestamatto.identityhub.infrastructure.messaging.template.email;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.EmailRenderer;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.email.RenderedEmail;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.EmailMessageTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * EmailRenderer implementation that resolves an EmailTemplate from the notification
 * message and renders a provider-ready email object.
 */
public class TemplatedEmailRenderer implements EmailRenderer {

    private final Map<EmailMessageTemplate, EmailTemplate> emailTemplates;

    public TemplatedEmailRenderer(List<EmailTemplate> emailTemplates) {
        this.emailTemplates = emailTemplates.stream()
                .collect(Collectors.toMap(EmailTemplate::supports, Function.identity()));
    }

    @Override
    public RenderedEmail render(NotificationMessage notificationMessage) {
        var template = resolveEmailtemplate(notificationMessage);
        var subject = notificationMessage.details().getOrDefault("subject", "IdentityHub notification");
        var body = template.render(notificationMessage);

        return new RenderedEmail(notificationMessage.recipient(), subject, body);
    }

    public EmailTemplate resolveEmailtemplate(NotificationMessage notificationMessage) {
        var messageTemplates = notificationMessage.messageTemplates();

        return Optional.ofNullable(emailTemplates.get(messageTemplates.emailMessageTemplate()))
                .orElseThrow(() -> new IllegalArgumentException("Email template not found: " + messageTemplates.emailMessageTemplate()));
    }
}
