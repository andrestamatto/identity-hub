package br.dev.andrestamatto.identityhub.interfaces.rest.controller;

import br.dev.andrestamatto.identityhub.application.usecase.PasswordLogin;
import br.dev.andrestamatto.identityhub.domain.model.RawPassword;
import br.dev.andrestamatto.identityhub.interfaces.rest.dto.LoginResponse;
import br.dev.andrestamatto.identityhub.interfaces.rest.dto.PasswordLoginRequest;
import br.dev.andrestamatto.identityhub.interfaces.rest.mapper.AuthResponseMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthenticateController {

    private final PasswordLogin passwordLoginUseCase;
    private final AuthResponseMapper authResponseMapper;

    public AuthenticateController(PasswordLogin login, AuthResponseMapper authResponseMapper) {
        this.passwordLoginUseCase = login;
        this.authResponseMapper = authResponseMapper;
    }

    @PostMapping(value="/login", consumes = "application/json")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody PasswordLoginRequest loginRequest) {
        var loginResult = passwordLoginUseCase.execute(loginRequest.identity(), RawPassword.from(loginRequest.password()));
        return ResponseEntity.ok().body(
                authResponseMapper.toLoginResponse(loginResult)
                );

    }
}
