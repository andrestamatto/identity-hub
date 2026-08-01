package br.dev.andrestamatto.identityhub.communication.application;

import java.util.UUID;

@FunctionalInterface
public interface EmailOriginResolver {

    EmailOrigin resolve(UUID applicationId);
}
