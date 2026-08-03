package br.dev.andrestamatto.identityhub.access.application;

public interface MembershipGrantRepository {

    MembershipGrantOperation addOrReplay(MembershipGrantOperation proposed);
}
