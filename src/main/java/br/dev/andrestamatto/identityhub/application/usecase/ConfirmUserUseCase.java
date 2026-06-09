package br.dev.andrestamatto.identityhub.application.usecase;

import br.dev.andrestamatto.identityhub.application.events.UserConfirmedEvent;
import br.dev.andrestamatto.identityhub.application.exceptions.UserNotFoundException;
import br.dev.andrestamatto.identityhub.domain.exceptions.UserStatusDoesNotMatchRegistrationConfirmationException;
import br.dev.andrestamatto.identityhub.application.ports.input.command.ConfirmUserCommand;
import br.dev.andrestamatto.identityhub.application.ports.output.DomainEventPublisher;
import br.dev.andrestamatto.identityhub.application.ports.output.UserRepository;
import br.dev.andrestamatto.identityhub.domain.entities.User;
import br.dev.andrestamatto.identityhub.domain.valueobjects.UserStatus;
import br.dev.andrestamatto.identityhub.domain.valueobjects.Username;
import br.dev.andrestamatto.identityhub.domain.valueobjects.UsernameType;
import br.dev.andrestamatto.identityhub.domain.valueobjects.VerificationToken;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

public class ConfirmUserUseCase implements ConfirmUser {

    private final UserRepository userRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;

    public ConfirmUserUseCase(UserRepository userRepository, DomainEventPublisher domainEventPublisher, Clock clock) {
        this.userRepository = userRepository;
        this.domainEventPublisher = domainEventPublisher;
        this.clock = clock;
    }

    @Override
    public void execute(ConfirmUserCommand confirmUserCommand) {

        Username username = Username.create(confirmUserCommand.username(), UsernameType.UNKNOWN);
        User foundUser = Optional.ofNullable(userRepository.findByUsername(username))
                .orElseThrow(UserNotFoundException::new);

        if ( !UserStatus.PENDING_VERIFICATION.equals(foundUser.status()) || foundUser.verificationToken() == null || foundUser.verificationToken().code() == null) {
            throw new UserStatusDoesNotMatchRegistrationConfirmationException();
        }

        if (
            VerificationToken.validateCode(
                    foundUser.verificationToken(), confirmUserCommand.verificationCode(), Instant.now(clock)
            )
        ) {

            User activeUser = userRepository.save(
                    User.activate(foundUser)
            );

            if (UserStatus.ACTIVE.equals(activeUser.status()) && activeUser.verificationToken() == null) {
                domainEventPublisher.publish(new UserConfirmedEvent(activeUser.username()));
            }
        }

    }
}
