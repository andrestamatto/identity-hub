package br.dev.andrestamatto.identityhub.clientapplication.application;

import java.util.Optional;

public interface MembershipProvisioningClientResolver {

    Optional<MembershipProvisioningClient> resolve(String authorizedParty);
}
