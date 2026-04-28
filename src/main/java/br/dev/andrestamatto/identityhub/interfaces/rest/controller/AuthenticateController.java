package br.dev.andrestamatto.identityhub.interfaces.rest.controller;

import br.dev.andrestamatto.identityhub.application.usecase.Authenticatable;
import br.dev.andrestamatto.identityhub.domain.model.RawPassword;
import br.dev.andrestamatto.identityhub.interfaces.rest.dto.AuthenticatableResponse;
import br.dev.andrestamatto.identityhub.interfaces.rest.dto.LoginRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthenticateController {

    private final Authenticatable login;

    public AuthenticateController(Authenticatable login) {
        this.login = login;
    }

    @PostMapping(value="/login", consumes = "application/json")
    public ResponseEntity<AuthenticatableResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok().body(
                    login.execute(loginRequest.identity(), RawPassword.from(loginRequest.password()))
                );

    }
}
