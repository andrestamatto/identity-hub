package br.dev.andrestamatto.identityhub.application.usecase;

import br.dev.andrestamatto.identityhub.application.ports.input.command.ConfirmUserCommand;
import br.dev.andrestamatto.identityhub.domain.entities.User;

public interface ConfirmUser {
    User execute(ConfirmUserCommand confirmUserCommand);
}
