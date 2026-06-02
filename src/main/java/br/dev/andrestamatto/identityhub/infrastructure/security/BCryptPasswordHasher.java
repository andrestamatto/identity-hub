package br.dev.andrestamatto.identityhub.infrastructure.security;

import br.dev.andrestamatto.identityhub.application.ports.output.PasswordHasher;
import br.dev.andrestamatto.identityhub.domain.valueobjects.EncodedPassword;
import br.dev.andrestamatto.identityhub.domain.valueobjects.RawPassword;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private final SecurityProperties securityProperties;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public BCryptPasswordHasher(SecurityProperties securityProperties, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.securityProperties = securityProperties;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    @Override
    public EncodedPassword hashRawPassword(RawPassword rawPassword) {
        var spicyRawPassword = applyPepper(rawPassword);
        var encodedStringPassword = bCryptPasswordEncoder.encode(spicyRawPassword.value());

        validate(encodedStringPassword,  spicyRawPassword);

        return new EncodedPassword(encodedStringPassword);
    }

    private RawPassword applyPepper(RawPassword rawPassword) {
        var spicyPassword = DigestUtils.sha256Hex(rawPassword.value() + securityProperties.apiSecret());
        return new RawPassword(spicyPassword);
    }

    private void validate(String encodedStringPassword, RawPassword spicyRawPassword) {
        if (!bCryptPasswordEncoder.matches(spicyRawPassword.value(), encodedStringPassword)) {
            throw new IllegalStateException("Something went wrong on hashing raw encodedPassword: encodedPassword does not match.");
        };
    }

}
