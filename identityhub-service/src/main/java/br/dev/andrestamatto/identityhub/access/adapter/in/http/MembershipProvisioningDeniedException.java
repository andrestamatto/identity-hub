package br.dev.andrestamatto.identityhub.access.adapter.in.http;

final class MembershipProvisioningDeniedException extends RuntimeException {

    MembershipProvisioningDeniedException() {
        super("The authenticated client cannot provision memberships");
    }
}
