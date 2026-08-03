package br.dev.andrestamatto.identityhub.clientapplication.application;

import java.util.Objects;
import java.util.UUID;

public record MembershipProvisioningClient(
        UUID applicationId,
        UUID applicationClientId) {

    public MembershipProvisioningClient {
        Objects.requireNonNull(applicationId);
        Objects.requireNonNull(applicationClientId);
    }
}
