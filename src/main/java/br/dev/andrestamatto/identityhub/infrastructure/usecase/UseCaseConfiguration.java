package br.dev.andrestamatto.identityhub.infrastructure.usecase;

import br.dev.andrestamatto.identityhub.application.ports.output.PasswordHasher;
import br.dev.andrestamatto.identityhub.application.ports.output.UserRegistrationPolicy;
import br.dev.andrestamatto.identityhub.application.ports.output.UserRepository;
import br.dev.andrestamatto.identityhub.application.usecase.RegisterUser;
import br.dev.andrestamatto.identityhub.application.usecase.RegisterUserUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class UseCaseConfiguration {

    @Bean
    public RegisterUser registerUser(
            UserRegistrationPolicy userRegistrationPolicy,
            PasswordHasher passwordHasher,
            UserRepository userRepository,
            Clock clock
    ) {
        return new RegisterUserUseCase(userRegistrationPolicy, passwordHasher, userRepository, clock);
    }
}
