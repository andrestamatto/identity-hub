package br.dev.andrestamatto.identityhub.infrastructure.security.password;

import br.dev.andrestamatto.identityhub.domain.model.EncodedPassword;
import br.dev.andrestamatto.identityhub.domain.model.RawPassword;
import br.dev.andrestamatto.identityhub.domain.service.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCryptPasswordEncoderAdapter implements PasswordEncoder {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public EncodedPassword encode(RawPassword rawPassword) {
        return EncodedPassword.from(encoder.encode(rawPassword.value()));
    }

    @Override
    public boolean matches(RawPassword rawPassword, EncodedPassword encodedPassword) {
        return encoder.matches(rawPassword.value(), encodedPassword.value());
    }

}
