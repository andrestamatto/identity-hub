package br.dev.andrestamatto.identityhub.application.usecase;

import br.dev.andrestamatto.identityhub.application.ports.input.command.ConfirmUserCommand;

public interface ConfirmUser {
    void execute(ConfirmUserCommand confirmUserCommand);
}
