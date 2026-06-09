package br.dev.andrestamatto.identityhub.application.usecase;

import br.dev.andrestamatto.identityhub.application.events.UserRegisteredPendingVerificationEvent;
import br.dev.andrestamatto.identityhub.application.exceptions.UserAlreadyExistsException;
import br.dev.andrestamatto.identityhub.application.ports.input.command.RegisterUserCommand;
import br.dev.andrestamatto.identityhub.application.ports.output.DomainEventPublisher;
import br.dev.andrestamatto.identityhub.application.ports.output.PasswordHasher;
import br.dev.andrestamatto.identityhub.application.ports.output.UserRegistrationPolicy;
import br.dev.andrestamatto.identityhub.application.ports.output.UserRepository;
import br.dev.andrestamatto.identityhub.application.ports.output.VerificationTokenGenerator;
import br.dev.andrestamatto.identityhub.domain.entities.User;
import br.dev.andrestamatto.identityhub.domain.valueobjects.*;

import java.time.Clock;
import java.time.Instant;

public class RegisterUserUseCase implements RegisterUser {

    private final UserRegistrationPolicy registrationPolicy;
    private final DomainEventPublisher domainEventPublisher;
    private final PasswordHasher passwordHasher;
    private final UserRepository userRepository;
    private final Clock clock;
    private final VerificationTokenGenerator verificationTokenGenerator;

    public RegisterUserUseCase(UserRegistrationPolicy registrationPolicy, DomainEventPublisher domainEventPublisher, PasswordHasher passwordHasher, UserRepository userRepository, Clock clock, VerificationTokenGenerator verificationTokenGenerator) {
        this.registrationPolicy = registrationPolicy;
        this.domainEventPublisher = domainEventPublisher;
        this.passwordHasher = passwordHasher;
        this.userRepository = userRepository;
        this.clock = clock;
        this.verificationTokenGenerator = verificationTokenGenerator;
    }

    @Override
    public User execute(RegisterUserCommand command) {

        var username = Username.create(command.username());

        if ( userRepository.existsBy(username) ) {
            throw new UserAlreadyExistsException();
        }

        var encodedPassword = passwordHasher.hashRawPassword(
                RawPassword.create(command.rawPassword())
        );

        var initialUserStatus = registrationPolicy.initialStatusFor(username.usernameType());

        var verificationToken = generateToken(username, initialUserStatus);

        var registeredUser = userRepository.save(
                User.register(username, encodedPassword, initialUserStatus , Instant.now(clock), verificationToken)
        );

        if (UserStatus.PENDING_VERIFICATION.equals(registeredUser.status()) && registeredUser.verificationToken() != null) {
            domainEventPublisher.publish(new UserRegisteredPendingVerificationEvent(
                    registeredUser.username(),
                    registeredUser.verificationToken()
            ));
        }

        return registeredUser;
    }

    private VerificationToken generateToken(Username username, UserStatus initialStatus) {

        if ( !UserStatus.PENDING_VERIFICATION.equals(initialStatus) ) {
            return null;
        }

        var verificationMethod = switch (username.usernameType()) {
            case EMAIL -> NotificationMethod.EMAIL;
            case PHONE -> NotificationMethod.SMS;
            case EMAIL_OR_PHONE -> NotificationMethod.BOTH;
            case EXTERNAL_ID -> throw new UnsupportedOperationException("ExternalId is not supported yet.");
            case UNKNOWN -> throw new UnsupportedOperationException("Unknown user type.");
        };

        return verificationTokenGenerator.generate(verificationMethod);

    }
}
