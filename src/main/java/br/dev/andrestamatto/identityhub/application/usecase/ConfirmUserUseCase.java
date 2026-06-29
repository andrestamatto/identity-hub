package br.dev.andrestamatto.identityhub.application.usecase;

import br.dev.andrestamatto.identityhub.application.events.UserConfirmedEvent;
import br.dev.andrestamatto.identityhub.application.exceptions.UserNotFoundException;
import br.dev.andrestamatto.identityhub.application.ports.input.command.ConfirmUserCommand;
import br.dev.andrestamatto.identityhub.application.ports.output.DomainEventPublisher;
import br.dev.andrestamatto.identityhub.application.ports.output.UserRepository;
import br.dev.andrestamatto.identityhub.application.ports.output.UsernameResolver;
import br.dev.andrestamatto.identityhub.domain.entities.User;
import br.dev.andrestamatto.identityhub.domain.exceptions.UserStatusDoesNotMatchRegistrationConfirmationException;
import br.dev.andrestamatto.identityhub.domain.valueobjects.UserStatus;
import br.dev.andrestamatto.identityhub.domain.valueobjects.Username;
import br.dev.andrestamatto.identityhub.domain.valueobjects.VerificationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

public class ConfirmUserUseCase implements ConfirmUser {

    private static final Logger log = LoggerFactory.getLogger(ConfirmUserUseCase.class);

    private final UserRepository userRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;
    private final UsernameResolver usernameResolver;

    public ConfirmUserUseCase(UserRepository userRepository, DomainEventPublisher domainEventPublisher, Clock clock, UsernameResolver usernameResolver) {
        this.userRepository = userRepository;
        this.domainEventPublisher = domainEventPublisher;
        this.clock = clock;
        this.usernameResolver = usernameResolver;
    }

    @Override
    public User execute(ConfirmUserCommand confirmUserCommand) {

        Username username = usernameResolver.resolve(confirmUserCommand.username());
        log.info("Confirm user registration requested.");
        User foundUser = Optional.ofNullable(userRepository.findByUsername(username))
                .orElseThrow(UserNotFoundException::new);

        if ( !UserStatus.PENDING_VERIFICATION.equals(foundUser.status()) || foundUser.verificationToken() == null || foundUser.verificationToken().code() == null) {
            throw new UserStatusDoesNotMatchRegistrationConfirmationException();
        }

        VerificationToken.validateCode(
                foundUser.verificationToken(), confirmUserCommand.verificationCode(), Instant.now(clock)
        );

        User activeUser = userRepository.save(
                User.activate(foundUser)
        );

        log.info("User registration confirmed. userId={} status={}", activeUser.uuid(), activeUser.status());

        if (UserStatus.ACTIVE.equals(activeUser.status()) && activeUser.verificationToken() == null) {
            log.info("Publishing user confirmed event. userId={} usernameType={}", activeUser.uuid(), activeUser.username().usernameType());
            domainEventPublisher.publish(new UserConfirmedEvent(activeUser.username()));
        }

        return activeUser;
    }
}
