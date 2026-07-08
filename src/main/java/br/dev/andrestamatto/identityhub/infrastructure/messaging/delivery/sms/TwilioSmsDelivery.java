package br.dev.andrestamatto.identityhub.infrastructure.messaging.delivery.sms;

import br.dev.andrestamatto.identityhub.application.exceptions.SmsDeliveryException;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.delivery.SmsDelivery;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.sms.SmsContent;
import br.dev.andrestamatto.identityhub.infrastructure.messaging.config.NotificationProperties;
import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class TwilioSmsDelivery implements SmsDelivery {

    private final NotificationProperties.Providers smsProviders;

    public TwilioSmsDelivery(NotificationProperties.Providers smsProviders) {
        this.smsProviders = smsProviders;
    }

    @Override
    public void deliver(SmsContent sms) {
        try {
            var twilio = smsProviders.twilio();
            Twilio.init(twilio.accountSid(), twilio.authToken());

            Message.creator(
                    new PhoneNumber(sms.to()),
                    new PhoneNumber(twilio.from()),
                    sms.body()
            ).create();
        } catch (ApiException exception) {
            throw new SmsDeliveryException("SMS delivery failed through Twilio provider.", exception);
        }

    }

}
