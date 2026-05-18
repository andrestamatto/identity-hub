package br.dev.andrestamatto.identityhub.application.usecase;

import br.dev.andrestamatto.identityhub.application.exceptions.UserAlreadyExistsException;
import br.dev.andrestamatto.identityhub.application.ports.input.command.RegisterUserCommand;
import br.dev.andrestamatto.identityhub.application.ports.output.PasswordHasher;
import br.dev.andrestamatto.identityhub.application.repository.UserRepository;
import br.dev.andrestamatto.identityhub.domain.entities.User;
import br.dev.andrestamatto.identityhub.domain.valueobjects.Credentials;
import br.dev.andrestamatto.identityhub.domain.valueobjects.EncodedPassword;
import br.dev.andrestamatto.identityhub.domain.valueobjects.UserStatus;
import br.dev.andrestamatto.identityhub.support.CredentialsTestData;
import br.dev.andrestamatto.identityhub.support.UserTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class RegisterUserUseCaseTest {

    private UserRepository registerUserRepository;
    private PasswordHasher passwordHasher;
    private Credentials validCredentials;
    private RegisterUserCommand validUserCommand;
    private EncodedPassword hashedPassword;
    private Clock fixedClock;

    @BeforeEach
    public void setup() {
        registerUserRepository = mock(UserRepository.class);
        passwordHasher = mock(PasswordHasher.class);
        validCredentials = CredentialsTestData.valid();
        validUserCommand = new RegisterUserCommand(
                validCredentials.username().value(),
                validCredentials.rawPassword().value()
        );
        hashedPassword = new EncodedPassword(UserTestData.validEncodedPasswordString);
        fixedClock = Clock.fixed(Instant.parse("2026-05-18T10:00:00Z"), ZoneOffset.UTC);

        when(passwordHasher.hashRawPassword(validCredentials.rawPassword())).thenReturn(hashedPassword);
    }

    @Test
    public void IH001ShouldRegisterUserWhenUsernameIsAvailable() {
        var registerUserUseCase = new RegisterUserUseCase(passwordHasher, registerUserRepository, fixedClock);
        User registeredUser = UserTestData.registered();

        when(registerUserRepository.existsBy(validCredentials.username())).thenReturn(false);
        when(registerUserRepository.save(any(User.class))).thenReturn(registeredUser);

        User resultTestUser = assertDoesNotThrow(() -> registerUserUseCase.register(validUserCommand));

        assertNotNull(resultTestUser);
        assertEquals(registeredUser, resultTestUser);
        verify(registerUserRepository).existsBy(validCredentials.username());
        verify(registerUserRepository).save(any(User.class));
    }

    @Test
    public void IH001ShouldRejectWhenUsernameAlreadyExists() {
        var registerUserUseCase = new RegisterUserUseCase(passwordHasher, registerUserRepository, fixedClock);

        when(registerUserRepository.existsBy(any())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> registerUserUseCase.register(validUserCommand));
        verify(registerUserRepository, never()).save(any(User.class));
    }

    @Test
    public void IH001ShouldStoreOnlyEncodedPassword() {
        var registerUserUseCase = new RegisterUserUseCase(passwordHasher, registerUserRepository, fixedClock);

        when(registerUserRepository.existsBy(validCredentials.username())).thenReturn(false);
        when(registerUserRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0, User.class));

        User resultTestUser = assertDoesNotThrow(() -> registerUserUseCase.register(validUserCommand));

        var savedUserCaptor = ArgumentCaptor.forClass(User.class);
        verify(registerUserRepository).save(savedUserCaptor.capture());
        User savedUser = savedUserCaptor.getValue();

        assertNotNull(savedUser.encodedPassword());
        assertEquals(hashedPassword.value(), savedUser.encodedPassword().value());
        assertNotEquals(validCredentials.rawPassword().value(), savedUser.encodedPassword().value());
        assertEquals(hashedPassword.value(), resultTestUser.encodedPassword().value());
    }

    @Test
    public void IH001ShouldCreateUserWithActiveStatus() {
        var registerUserUseCase = new RegisterUserUseCase(passwordHasher, registerUserRepository, fixedClock);

        when(registerUserRepository.existsBy(validCredentials.username())).thenReturn(false);
        when(registerUserRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0, User.class));

        User resultTestUser = assertDoesNotThrow(() -> registerUserUseCase.register(validUserCommand));

        var savedUserCaptor = ArgumentCaptor.forClass(User.class);
        verify(registerUserRepository).save(savedUserCaptor.capture());
        User savedUser = savedUserCaptor.getValue();

        assertEquals(UserStatus.ACTIVE, savedUser.status());
        assertEquals(UserStatus.ACTIVE, resultTestUser.status());
    }
}
