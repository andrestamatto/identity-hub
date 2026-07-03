package br.dev.andrestamatto.identityhub.application.ports.output.messaging.delivery;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.sms.RenderedSms;

/**
 * Output port for delivering already rendered SMS messages.
 * Implementations represent provider/transport choices such as Twilio, Zenvia,
 * AWS SNS, or a local logging adapter.
 */
public interface SmsDelivery {
    void deliver(RenderedSms sms);
}
