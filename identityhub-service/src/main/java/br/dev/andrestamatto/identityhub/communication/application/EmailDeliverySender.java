package br.dev.andrestamatto.identityhub.communication.application;

@FunctionalInterface
public interface EmailDeliverySender {

    void send(OutboundEmail email);
}
