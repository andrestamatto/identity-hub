package br.dev.andrestamatto.identityhub.infrastructure.usecase;

import br.dev.andrestamatto.identityhub.application.ports.output.*;
import br.dev.andrestamatto.identityhub.application.usecase.ConfirmUser;
import br.dev.andrestamatto.identityhub.application.usecase.ConfirmUserUseCase;
import br.dev.andrestamatto.identityhub.application.usecase.RegisterUser;
import br.dev.andrestamatto.identityhub.application.usecase.RegisterUserUseCase;
import br.dev.andrestamatto.identityhub.infrastructure.decorator.TransactionalConfirmUser;
import br.dev.andrestamatto.identityhub.infrastructure.decorator.TransactionalRegisterUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class UseCaseConfiguration {

    @Bean
    public ConfirmUser confirmUser(UserRepository userRepository, DomainEventPublisher domainEventPublisher, Clock clock, UsernameResolver usernameResolver) {
        var confirmUserUseCase = new ConfirmUserUseCase(userRepository,  domainEventPublisher, clock, usernameResolver);
        return new TransactionalConfirmUser(confirmUserUseCase);
    }

    @Bean
    public RegisterUser registerUser(
            UserRegistrationPolicy userRegistrationPolicy,
            DomainEventPublisher domainEventPublisher,
            PasswordHasher passwordHasher,
            UserRepository userRepository,
            Clock clock,
            VerificationTokenGenerator verificationTokenGenerator,
            UsernameResolver usernameResolver
    ) {
        var registerUserUseCase = new RegisterUserUseCase(
                userRegistrationPolicy,
                domainEventPublisher,
                passwordHasher,
                userRepository,
                clock,
                verificationTokenGenerator,
                usernameResolver
        );

        return new TransactionalRegisterUser(registerUserUseCase);
    }
}
