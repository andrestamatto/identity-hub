package br.dev.andrestamatto.identityhub.access.application;

public final class MembershipGrantConflictException extends RuntimeException {

    public MembershipGrantConflictException() {
        super("Idempotency key is already assigned to another membership grant");
    }
}
