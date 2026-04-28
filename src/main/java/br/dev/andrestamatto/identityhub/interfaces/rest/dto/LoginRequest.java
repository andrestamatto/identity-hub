package br.dev.andrestamatto.identityhub.interfaces.rest.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest (
        @JsonAlias("email")
        @NotBlank(message = "Identity is required.")
        String identity,
        @NotBlank(message = "Password is required.")
        String password
) implements AuthenticatableRequest {}
