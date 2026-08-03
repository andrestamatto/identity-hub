package br.dev.andrestamatto.identityhub.access.domain;

import java.util.Objects;
import java.util.UUID;

public record MembershipApplicationRef(UUID value) {

    public MembershipApplicationRef {
        Objects.requireNonNull(value);
    }
}
