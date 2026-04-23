package br.dev.andrestamatto.identityhub.interfaces.rest.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping(value="/me", produces = "application/json")
    public String me() {
        return "Usuário autenticado";
    }

}
