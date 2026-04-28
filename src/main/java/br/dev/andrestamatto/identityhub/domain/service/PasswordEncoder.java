package br.dev.andrestamatto.identityhub.domain.service;

import br.dev.andrestamatto.identityhub.domain.model.EncodedPassword;
import br.dev.andrestamatto.identityhub.domain.model.RawPassword;

public interface PasswordEncoder {
    EncodedPassword encode(RawPassword rawPassword);
    boolean matches(RawPassword rawPassword, EncodedPassword encodedPassword);
}
