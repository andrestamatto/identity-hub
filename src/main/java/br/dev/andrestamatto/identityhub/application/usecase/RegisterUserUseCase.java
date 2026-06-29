package br.dev.andrestamatto.identityhub.application.usecase;

import br.dev.andrestamatto.identityhub.application.events.UserRegisteredPendingVerificationEvent;
import br.dev.andrestamatto.identityhub.application.exceptions.UserAlreadyExistsException;
import br.dev.andrestamatto.identityhub.application.ports.input.command.RegisterUserCommand;
import br.dev.andrestamatto.identityhub.application.ports.output.*;
import br.dev.andrestamatto.identityhub.domain.entities.User;
import br.dev.andrestamatto.identityhub.domain.valueobjects.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;

public class RegisterUserUseCase implements RegisterUser {

    private static final Logger log = LoggerFactory.getLogger(RegisterUserUseCase.class);

    private final UserRegistrationPolicy registrationPolicy;
    private final DomainEventPublisher domainEventPublisher;
    private final PasswordHasher passwordHasher;
    private final UserRepository userRepository;
    private final Clock clock;
    private final VerificationTokenGenerator verificationTokenGenerator;
    private final UsernameResolver usernameResolver;

    public RegisterUserUseCase(UserRegistrationPolicy registrationPolicy, DomainEventPublisher domainEventPublisher, PasswordHasher passwordHasher, UserRepository userRepository, Clock clock, VerificationTokenGenerator verificationTokenGenerator, UsernameResolver usernameResolver) {
        this.registrationPolicy = registrationPolicy;
        this.domainEventPublisher = domainEventPublisher;
        this.passwordHasher = passwordHasher;
        this.userRepository = userRepository;
        this.clock = clock;
        this.verificationTokenGenerator = verificationTokenGenerator;
        this.usernameResolver = usernameResolver;
    }

    @Override
    public User execute(RegisterUserCommand command) {

        var username = usernameResolver.resolve(command.username());
        log.info("Register user requested. usernameType={}", username.usernameType());

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

        log.info(
                "User registered. userId={} usernameType={} status={}",
                registeredUser.uuid(),
                registeredUser.username().usernameType(),
                registeredUser.status()
        );

        if (UserStatus.PENDING_VERIFICATION.equals(registeredUser.status()) && registeredUser.verificationToken() != null) {
            log.info(
                    "Publishing pending verification event. userId={} usernameType={} notificationMethod={}",
                    registeredUser.uuid(),
                    registeredUser.username().usernameType(),
                    registeredUser.verificationToken().method()
            );
            domainEventPublisher.publish(new UserRegisteredPendingVerificationEvent(
                    registeredUser.username(),
                    registeredUser.verificationToken()
            ));
        }

        return registeredUser;
    }

    private VerificationToken generateToken(Username username, UserStatus initialStatus) {

        if ( !UserStatus.PENDING_VERIFICATION.equals(initialStatus) ) {
            log.debug("Verification token not generated because initial status is {}.", initialStatus);
            return null;
        }

        var verificationMethod = switch (username.usernameType()) {
            case EMAIL -> NotificationMethod.EMAIL;
            case PHONE -> NotificationMethod.SMS;
        };

        log.info("Generating verification token. usernameType={} notificationMethod={}", username.usernameType(), verificationMethod);
        return verificationTokenGenerator.generate(verificationMethod);

    }
}
