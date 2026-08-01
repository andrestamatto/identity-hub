package br.dev.andrestamatto.identityhub.communication.application;

import br.dev.andrestamatto.identityhub.communication.domain.EmailRecipient;
import java.util.Objects;

public record OutboundEmail(EmailRecipient recipient, String subject, String body) {

    public OutboundEmail {
        Objects.requireNonNull(recipient);
        Objects.requireNonNull(subject);
        Objects.requireNonNull(body);
    }
}
