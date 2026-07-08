package br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp;

public sealed interface WhatsappContent permits WhatsappTextContent, WhatsappMediaContent {
    WhatsappMediaType mediaType();
}
