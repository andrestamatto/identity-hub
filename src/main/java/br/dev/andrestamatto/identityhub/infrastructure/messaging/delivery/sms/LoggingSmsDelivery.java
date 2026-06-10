package br.dev.andrestamatto.identityhub.infrastructure.messaging.delivery.sms;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.delivery.SmsDelivery;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.sms.RenderedSms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development SMS delivery adapter.
 * It logs that an SMS would be delivered without calling an external provider.
 */
public class LoggingSmsDelivery implements SmsDelivery {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsDelivery.class);

    @Override
    public void deliver(RenderedSms sms) {
        log.info("SMS delivery simulated. bodyLength={}", sms.body().length());
    }
}
