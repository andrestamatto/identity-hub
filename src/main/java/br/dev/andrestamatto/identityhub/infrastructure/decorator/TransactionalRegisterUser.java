package br.dev.andrestamatto.identityhub.infrastructure.decorator;

import br.dev.andrestamatto.identityhub.application.ports.input.command.RegisterUserCommand;
import br.dev.andrestamatto.identityhub.application.usecase.RegisterUser;
import br.dev.andrestamatto.identityhub.domain.entities.User;
import jakarta.transaction.Transactional;

public class TransactionalRegisterUser implements RegisterUser {

    private final RegisterUser delegate;

    public TransactionalRegisterUser(RegisterUser delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public User execute(RegisterUserCommand command) {
        return delegate.execute(command);
    }
}
