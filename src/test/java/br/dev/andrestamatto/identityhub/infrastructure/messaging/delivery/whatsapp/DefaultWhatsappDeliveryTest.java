package br.dev.andrestamatto.identityhub.infrastructure.messaging.delivery.whatsapp;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp.RenderedWhatsapp;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp.WhatsappMediaType;
import br.dev.andrestamatto.identityhub.infrastructure.apis.WhatsappApiClient;
import br.dev.andrestamatto.identityhub.infrastructure.apis.WhatsappRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

        var requestCaptor = ArgumentCaptor.forClass(WhatsappRequest.class);
        verify(apiClient).send(requestCaptor.capture());
        verify(apiClient, never()).sendMedia(org.mockito.ArgumentMatchers.any());

        var request = requestCaptor.getValue();
        assertEquals("+5511999998888", request.number());
        assertEquals("text", request.mediaType());
        assertEquals("IdentityHub code: 123456", request.caption());
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

        var requestCaptor = ArgumentCaptor.forClass(WhatsappRequest.class);
        verify(apiClient).sendMedia(requestCaptor.capture());
        verify(apiClient, never()).send(org.mockito.ArgumentMatchers.any());

        var request = requestCaptor.getValue();
        assertEquals("+5511999998888", request.number());
        assertEquals("image", request.mediaType());
        assertEquals("https://identityhub.dev/media/whatsapp/identityhub.logo.png", request.mediaUrl());
        assertEquals("IdentityHub code: 123456", request.caption());
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
