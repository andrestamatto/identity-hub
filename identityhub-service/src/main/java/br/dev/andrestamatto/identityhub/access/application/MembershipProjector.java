package br.dev.andrestamatto.identityhub.access.application;

import br.dev.andrestamatto.identityhub.access.domain.Membership;

@FunctionalInterface
public interface MembershipProjector {

    void project(Membership membership);
}
