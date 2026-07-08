package br.dev.andrestamatto.identityhub.infrastructure.messaging.delivery.whatsapp;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp.WhatsappMediaContent;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp.WhatsappMediaType;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp.WhatsappTextContent;
import br.dev.andrestamatto.identityhub.infrastructure.apis.WhatsappApiClient;
import br.dev.andrestamatto.identityhub.infrastructure.apis.WhatsappResponse;
import br.dev.andrestamatto.identityhub.infrastructure.apis.request.WhatsappMediaRequest;
import br.dev.andrestamatto.identityhub.infrastructure.apis.request.WhatsappTextRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultWhatsappDeliveryTest {

    @Test
    public void shouldSendTextMessageThroughTextEndpoint() {
        var apiClient = mock(WhatsappApiClient.class);
        var delivery = new DefaultWhatsappDelivery(apiClient);
        var rendered = new WhatsappTextContent(
                "+5511999998888",
                "IdentityHub code: 123456"
        );
        when(apiClient.send(org.mockito.ArgumentMatchers.any()))
                .thenReturn(ResponseEntity.ok(new WhatsappResponse(true, "+5511999998888")));

        delivery.deliver(rendered);

        var requestCaptor = ArgumentCaptor.forClass(WhatsappTextRequest.class);
        verify(apiClient).send(requestCaptor.capture());
        verify(apiClient, never()).sendMedia(org.mockito.ArgumentMatchers.any());

        var request = requestCaptor.getValue();
        assertEquals("+5511999998888", request.number());
        assertEquals("IdentityHub code: 123456", request.message());
    }

    @Test
    public void shouldSendImageMessageThroughMediaEndpoint() {
        var apiClient = mock(WhatsappApiClient.class);
        var delivery = new DefaultWhatsappDelivery(apiClient);
        var rendered = new WhatsappMediaContent(
                "+5511999998888",
                "IdentityHub code: 123456",
                "https://identityhub.dev/media/whatsapp/identityhub.logo.png",
                WhatsappMediaType.IMAGE
        );
        when(apiClient.sendMedia(org.mockito.ArgumentMatchers.any()))
                .thenReturn(ResponseEntity.ok(new WhatsappResponse(true, "+5511999998888")));

        delivery.deliver(rendered);

        var requestCaptor = ArgumentCaptor.forClass(WhatsappMediaRequest.class);
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
                () -> new WhatsappMediaContent(
                        "+5511999998888",
                        "IdentityHub code: 123456",
                        null,
                        WhatsappMediaType.IMAGE
                )
        );

        assertEquals("WhatsApp media URL is required for media messages.", exception.getMessage());
    }
}
