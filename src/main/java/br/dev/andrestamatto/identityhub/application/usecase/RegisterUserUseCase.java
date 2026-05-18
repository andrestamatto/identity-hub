package br.dev.andrestamatto.identityhub.application.usecase;

import br.dev.andrestamatto.identityhub.application.exceptions.UserAlreadyExistsException;
import br.dev.andrestamatto.identityhub.application.ports.input.command.RegisterUserCommand;
import br.dev.andrestamatto.identityhub.application.ports.output.PasswordHasher;
import br.dev.andrestamatto.identityhub.application.repository.UserRepository;
import br.dev.andrestamatto.identityhub.domain.entities.User;
import br.dev.andrestamatto.identityhub.domain.valueobjects.Credentials;
import br.dev.andrestamatto.identityhub.domain.valueobjects.UserStatus;

import java.time.Clock;
import java.time.Instant;

public class RegisterUserUseCase {

    private final PasswordHasher passwordHasher;
    private final UserRepository userRepository;
    private final Clock clock;

    public RegisterUserUseCase(PasswordHasher passwordHasher, UserRepository userRepository, Clock clock) {
        this.passwordHasher = passwordHasher;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    public User register(RegisterUserCommand command) {
        var credentials = Credentials.create(command.username(), command.rawPassword());

        if ( userRepository.existsBy(credentials.username()) ) {
            throw new UserAlreadyExistsException();
        }

        User userToRegister = User.builder()
                .username(credentials.username())
                .password(
                        passwordHasher.hashRawPassword(
                                credentials.rawPassword()
                        )
                )
                .createdAt(Instant.now(clock))
                .status(UserStatus.ACTIVE)
                .build();

        return userRepository.save(userToRegister);
    }
}
