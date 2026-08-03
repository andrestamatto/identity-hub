package br.dev.andrestamatto.identityhub.access.domain;

import java.util.Objects;
import java.util.UUID;

public record MembershipUserAccountRef(UUID value) {

    public MembershipUserAccountRef {
        Objects.requireNonNull(value);
    }
}
