package br.dev.andrestamatto.identityhub.application.usecase;

import br.dev.andrestamatto.identityhub.application.ports.input.command.RegisterUserCommand;
import br.dev.andrestamatto.identityhub.domain.entities.User;

public interface RegisterUser {
    User execute(RegisterUserCommand command);
}
