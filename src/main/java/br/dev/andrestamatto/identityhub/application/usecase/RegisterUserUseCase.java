package br.dev.andrestamatto.identityhub.application.usecase;

import br.dev.andrestamatto.identityhub.application.exceptions.UserAlreadyExistsException;
import br.dev.andrestamatto.identityhub.application.ports.input.UserRegistrationPolicy;
import br.dev.andrestamatto.identityhub.application.ports.input.command.RegisterUserCommand;
import br.dev.andrestamatto.identityhub.application.ports.output.PasswordHasher;
import br.dev.andrestamatto.identityhub.application.repository.UserRepository;
import br.dev.andrestamatto.identityhub.domain.entities.User;
import br.dev.andrestamatto.identityhub.domain.valueobjects.RawPassword;
import br.dev.andrestamatto.identityhub.domain.valueobjects.Username;

import java.time.Clock;
import java.time.Instant;

public class RegisterUserUseCase {

    private final UserRegistrationPolicy registrationPolicy;
    private final PasswordHasher passwordHasher;
    private final UserRepository userRepository;
    private final Clock clock;

    public RegisterUserUseCase(UserRegistrationPolicy registrationPolicy, PasswordHasher passwordHasher, UserRepository userRepository, Clock clock) {
        this.registrationPolicy = registrationPolicy;
        this.passwordHasher = passwordHasher;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    public User register(RegisterUserCommand command) {

        var username = Username.create(command.username());

        if ( userRepository.existsBy(username) ) {
            throw new UserAlreadyExistsException();
        }

        var encodedPassword = passwordHasher.hashRawPassword(
                RawPassword.create(command.rawPassword())
        );

        var initialUserStatus = registrationPolicy.initialStatusFor(username.usernameType());

        User userToRegister = User.register(username, encodedPassword, initialUserStatus , Instant.now(clock));

        return userRepository.save(userToRegister);
    }
}
