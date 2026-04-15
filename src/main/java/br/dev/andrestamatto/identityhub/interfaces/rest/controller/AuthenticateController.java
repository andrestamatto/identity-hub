package br.dev.andrestamatto.identityhub.interfaces.rest.controller;

import br.dev.andrestamatto.identityhub.application.usecase.Authenticatable;
import br.dev.andrestamatto.identityhub.interfaces.rest.dto.AuthenticatableResponse;
import br.dev.andrestamatto.identityhub.interfaces.rest.dto.LoginRequest;
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

    @PostMapping(value="/login", produces = "application/json", consumes = "application/json")
    public AuthenticatableResponse login(@RequestBody LoginRequest loginRequest) {
        return login.execute(loginRequest.email(), loginRequest.password());
    }

}
