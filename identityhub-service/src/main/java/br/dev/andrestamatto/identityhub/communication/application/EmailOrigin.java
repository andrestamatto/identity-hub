package br.dev.andrestamatto.identityhub.communication.application;

import java.util.Objects;
import java.util.UUID;

public record EmailOrigin(
        UUID applicationId,
        String applicationIdentifier,
        String applicationDisplayName,
        String environment) {

    public EmailOrigin {
        Objects.requireNonNull(applicationId);
        applicationIdentifier = requireText(applicationIdentifier, "Application identifier");
        applicationDisplayName = requireText(applicationDisplayName, "Application display name");
        environment = requireText(environment, "Environment");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value);
        var normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return normalized;
    }
}
