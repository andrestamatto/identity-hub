package br.dev.andrestamatto.identityhub.access.application;

import br.dev.andrestamatto.identityhub.access.domain.Membership;
import java.util.Objects;
import java.util.regex.Pattern;

public record MembershipProjectionTask(
        Membership membership,
        int attempts,
        String correlationId) {

    public MembershipProjectionTask {
        Objects.requireNonNull(membership);
        Objects.requireNonNull(correlationId);
        if (attempts < 0) {
            throw new IllegalArgumentException("Projection attempts cannot be negative");
        }
        if (!Pattern.matches("[A-Za-z0-9._-]{1,64}", correlationId)) {
            throw new IllegalArgumentException("Invalid projection correlation id");
        }
    }
}
