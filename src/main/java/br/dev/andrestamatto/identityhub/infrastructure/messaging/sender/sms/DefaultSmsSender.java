package br.dev.andrestamatto.identityhub.infrastructure.messaging.sender.sms;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.delivery.SmsDelivery;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.SmsRenderer;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.senders.SmsSender;

/**
 * Default SmsSender implementation that coordinates rendering and delivery.
 * It keeps message rendering separate from provider-specific SMS transport.
 */
public class DefaultSmsSender implements SmsSender {

    private final SmsRenderer smsRenderer;
    private final SmsDelivery smsDelivery;

    public DefaultSmsSender(SmsRenderer smsRenderer, SmsDelivery smsDelivery) {
        this.smsRenderer = smsRenderer;
        this.smsDelivery = smsDelivery;
    }

    @Override
    public void send(NotificationMessage notificationMessage) {
        var renderedSms = smsRenderer.render(notificationMessage);
        smsDelivery.deliver(renderedSms);
    }
}
