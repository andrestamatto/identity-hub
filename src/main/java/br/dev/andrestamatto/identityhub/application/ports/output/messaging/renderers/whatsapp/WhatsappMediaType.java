package br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.whatsapp;

public enum WhatsappMediaType {
    AUDIO("audio"),
    DOCUMENT("document"),
    IMAGE("image"),
    TEXT("text"),
    VIDEO("video");

    private final String value;

    WhatsappMediaType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
