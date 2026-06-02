package br.dev.andrestamatto.identityhub.application.ports.input.command;

public record RegisterUserCommand(
        String username,
        String rawPassword
) {
}
