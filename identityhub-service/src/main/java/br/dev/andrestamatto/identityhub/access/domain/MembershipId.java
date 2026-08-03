package br.dev.andrestamatto.identityhub.access.domain;

import java.util.Objects;
import java.util.UUID;

public record MembershipId(UUID value) {

    public MembershipId {
        Objects.requireNonNull(value);
    }
}
