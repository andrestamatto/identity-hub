package br.dev.andrestamatto.identityhub.infrastructure.messaging.delivery.whatsapp;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.delivery.WhatsappDelivery;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp.WhatsappContent;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp.WhatsappMediaContent;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp.WhatsappTextContent;
import br.dev.andrestamatto.identityhub.infrastructure.apis.WhatsappApiClient;
import br.dev.andrestamatto.identityhub.infrastructure.apis.request.WhatsappMediaRequest;
import br.dev.andrestamatto.identityhub.infrastructure.apis.request.WhatsappTextRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class DefaultWhatsappDelivery implements WhatsappDelivery {

    private static final Logger log = LoggerFactory.getLogger(DefaultWhatsappDelivery.class);

    private final WhatsappApiClient whatsappApiClient;

    public DefaultWhatsappDelivery(WhatsappApiClient whatsappApiClient) {
        this.whatsappApiClient = whatsappApiClient;
    }

    @Override
    public void deliver(WhatsappContent whatsappContent) {
        var response = switch (whatsappContent) {
            case WhatsappMediaContent media -> whatsappApiClient.sendMedia(WhatsappMediaRequest.from(media));
            case WhatsappTextContent text -> whatsappApiClient.send(WhatsappTextRequest.from(text));
        };

        Optional.ofNullable(response)
                .ifPresentOrElse(
                        validResponse -> log.info("Whatsapp delivery response. HttpStatusCode={}, message={}.",
                                validResponse.getStatusCode(),
                                validResponse.getBody()),
                        () -> {
                            throw new RuntimeException("Could not catch the whatsapp response call");
                        }
                );
    }
}
