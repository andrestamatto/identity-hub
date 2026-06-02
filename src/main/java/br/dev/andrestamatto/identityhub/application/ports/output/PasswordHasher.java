package br.dev.andrestamatto.identityhub.application.ports.output;

import br.dev.andrestamatto.identityhub.domain.valueobjects.EncodedPassword;
import br.dev.andrestamatto.identityhub.domain.valueobjects.RawPassword;

public interface PasswordHasher {
    EncodedPassword hashRawPassword(RawPassword rawPassword);
}
