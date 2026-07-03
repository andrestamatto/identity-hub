package br.dev.andrestamatto.identityhub.infrastructure.messaging.delivery.whatsapp;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp.RenderedWhatsapp;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp.WhatsappMediaType;
import br.dev.andrestamatto.identityhub.infrastructure.apis.WhatsappApiClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class DefaultWhatsappDeliveryTest {

    @Test
    public void shouldSendTextMessageThroughTextEndpoint() {
        var apiClient = mock(WhatsappApiClient.class);
        var delivery = new DefaultWhatsappDelivery(apiClient);
        var rendered = new RenderedWhatsapp(
                "+5511999998888",
                WhatsappMediaType.TEXT,
                null,
                "IdentityHub code: 123456"
        );

        delivery.deliver(rendered);

        verify(apiClient).send(rendered);
        verify(apiClient, never()).sendMedia(rendered);
    }

    @Test
    public void shouldSendImageMessageThroughMediaEndpoint() {
        var apiClient = mock(WhatsappApiClient.class);
        var delivery = new DefaultWhatsappDelivery(apiClient);
        var rendered = new RenderedWhatsapp(
                "+5511999998888",
                WhatsappMediaType.IMAGE,
                "https://identityhub.dev/media/whatsapp/identityhub.logo.png",
                "IdentityHub code: 123456"
        );

        delivery.deliver(rendered);

        verify(apiClient).sendMedia(rendered);
        verify(apiClient, never()).send(rendered);
    }

    @Test
    public void shouldRejectMediaMessageWithoutMediaUrl() {
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> new RenderedWhatsapp(
                        "+5511999998888",
                        WhatsappMediaType.IMAGE,
                        null,
                        "IdentityHub code: 123456"
                )
        );

        assertEquals("WhatsApp media URL is required for media messages.", exception.getMessage());
    }
}
