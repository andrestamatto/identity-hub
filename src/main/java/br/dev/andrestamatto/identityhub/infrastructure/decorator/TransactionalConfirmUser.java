package br.dev.andrestamatto.identityhub.infrastructure.decorator;

import br.dev.andrestamatto.identityhub.application.ports.input.command.ConfirmUserCommand;
import br.dev.andrestamatto.identityhub.application.usecase.ConfirmUser;
import br.dev.andrestamatto.identityhub.domain.entities.User;
import jakarta.transaction.Transactional;

public class TransactionalConfirmUser implements ConfirmUser {

    private final ConfirmUser delegate;

    public TransactionalConfirmUser(ConfirmUser delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public User execute(ConfirmUserCommand confirmUserCommand) {
        return delegate.execute(confirmUserCommand);
    }
}
