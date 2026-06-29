package br.dev.andrestamatto.identityhub.infrastructure.messaging.template.whatsapp;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.WhatsappRenderer;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp.RenderedWhatsapp;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.WhatsappMessageTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TemplatedWhatsappRenderer implements WhatsappRenderer {

    private final Map<WhatsappMessageTemplate, WhatsappTemplate> whatsappTemplates;

    public TemplatedWhatsappRenderer(List<WhatsappTemplate> whatsappTemplates) {
        this.whatsappTemplates = whatsappTemplates.stream()
                .collect(Collectors.toMap(WhatsappTemplate::supports, Function.identity()));
    }

    @Override
    public RenderedWhatsapp render(NotificationMessage notificationMessage) {
        var template = resolveWhatsappTemplate(notificationMessage);
        var body = template.render(notificationMessage);

        return new RenderedWhatsapp(notificationMessage.recipient(), body);
    }

    private WhatsappTemplate resolveWhatsappTemplate(NotificationMessage notificationMessage) {
        var messageTemplates = notificationMessage.messageTemplates();

        return Optional.ofNullable(whatsappTemplates.get(messageTemplates.whatsappMessageTemplate()))
                .orElseThrow(() -> new IllegalArgumentException("WhatsApp template not found: " + messageTemplates.whatsappMessageTemplate()));
    }
}
