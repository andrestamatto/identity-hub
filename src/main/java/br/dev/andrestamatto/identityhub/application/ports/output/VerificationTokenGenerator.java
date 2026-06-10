package br.dev.andrestamatto.identityhub.application.ports.output;

import br.dev.andrestamatto.identityhub.domain.valueobjects.NotificationMethod;
import br.dev.andrestamatto.identityhub.domain.valueobjects.VerificationToken;

public interface VerificationTokenGenerator {
    VerificationToken generate(NotificationMethod method);
}
