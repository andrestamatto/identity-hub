package br.dev.andrestamatto.identityhub.interfaces.rest.mapper;

import br.dev.andrestamatto.identityhub.application.result.AuthenticationResult;
import br.dev.andrestamatto.identityhub.interfaces.rest.dto.LoginResponse;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuthResponseMapper {
    public LoginResponse toLoginResponse(AuthenticationResult result) {
        return Optional.ofNullable(result)
                .map(res -> new LoginResponse(res.accessToken(), res.tokenType(), res.expiresIn()))
                .orElseThrow();

    }
}
