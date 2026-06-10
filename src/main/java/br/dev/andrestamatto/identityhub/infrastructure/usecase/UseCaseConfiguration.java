package br.dev.andrestamatto.identityhub.infrastructure.usecase;

import br.dev.andrestamatto.identityhub.application.ports.output.DomainEventPublisher;
import br.dev.andrestamatto.identityhub.application.ports.output.PasswordHasher;
import br.dev.andrestamatto.identityhub.application.ports.output.UserRegistrationPolicy;
import br.dev.andrestamatto.identityhub.application.ports.output.UserRepository;
import br.dev.andrestamatto.identityhub.application.ports.output.VerificationTokenGenerator;
import br.dev.andrestamatto.identityhub.application.usecase.ConfirmUser;
import br.dev.andrestamatto.identityhub.application.usecase.ConfirmUserUseCase;
import br.dev.andrestamatto.identityhub.application.usecase.RegisterUser;
import br.dev.andrestamatto.identityhub.application.usecase.RegisterUserUseCase;
import jakarta.transaction.Transactional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class UseCaseConfiguration {

    @Bean
    @Transactional
    public ConfirmUser confirmUser(UserRepository userRepository, DomainEventPublisher domainEventPublisher, Clock clock) {
        return new ConfirmUserUseCase(userRepository,  domainEventPublisher, clock);
    }

    @Bean
    @Transactional
    public RegisterUser registerUser(
            UserRegistrationPolicy userRegistrationPolicy,
            DomainEventPublisher domainEventPublisher,
            PasswordHasher passwordHasher,
            UserRepository userRepository,
            Clock clock,
            VerificationTokenGenerator verificationTokenGenerator
    ) {
        return new RegisterUserUseCase(
                userRegistrationPolicy,
                domainEventPublisher,
                passwordHasher,
                userRepository,
                clock,
                verificationTokenGenerator
        );
    }
}
