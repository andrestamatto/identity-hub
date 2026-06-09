package br.dev.andrestamatto.identityhub.application.ports.output;

public interface DomainEventPublisher {
    void publish(Object event);
}
