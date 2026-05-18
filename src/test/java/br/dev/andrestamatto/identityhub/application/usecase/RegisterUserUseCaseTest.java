package br.dev.andrestamatto.identityhub.application.usecase;

import br.dev.andrestamatto.identityhub.application.exceptions.UserAlreadyExistsException;
import br.dev.andrestamatto.identityhub.application.ports.input.command.RegisterUserCommand;
import br.dev.andrestamatto.identityhub.application.ports.output.PasswordHasher;
import br.dev.andrestamatto.identityhub.application.repository.UserRepository;
import br.dev.andrestamatto.identityhub.domain.entities.User;
import br.dev.andrestamatto.identityhub.domain.valueobjects.EncodedPassword;
import br.dev.andrestamatto.identityhub.domain.valueobjects.RawPassword;
import br.dev.andrestamatto.identityhub.domain.valueobjects.UserStatus;
import br.dev.andrestamatto.identityhub.domain.valueobjects.Username;
import br.dev.andrestamatto.identityhub.support.UserTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static br.dev.andrestamatto.identityhub.support.UserTestData.validRawPasswordString;
import static br.dev.andrestamatto.identityhub.support.UserTestData.validUsernameString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class RegisterUserUseCaseTest {

    private UserRepository registerUserRepository;
    private PasswordHasher passwordHasher;
    private RegisterUserCommand validUserCommand;
    private EncodedPassword hashedPassword;
    private Clock fixedClock;

    @BeforeEach
    public void setup() {
        registerUserRepository = mock(UserRepository.class);
        passwordHasher = mock(PasswordHasher.class);
        validUserCommand = new RegisterUserCommand(
                validUsernameString,
                validRawPasswordString
        );
        hashedPassword = new EncodedPassword(UserTestData.validEncodedPasswordString);
        fixedClock = Clock.fixed(Instant.parse("2026-05-18T10:00:00Z"), ZoneOffset.UTC);

        var validRawPassword = RawPassword.create(validRawPasswordString);
        when(passwordHasher.hashRawPassword(validRawPassword)).thenReturn(hashedPassword);
    }

    @Test
    public void IH001ShouldRegisterUserWhenUsernameIsAvailable() {
        var registerUserUseCase = new RegisterUserUseCase(passwordHasher, registerUserRepository, fixedClock);
        User registeredUser = UserTestData.registered();

        when(registerUserRepository.existsBy(Username.create(validUsernameString))).thenReturn(false);
        when(registerUserRepository.save(any(User.class))).thenReturn(registeredUser);

        User resultTestUser = assertDoesNotThrow(() -> registerUserUseCase.register(validUserCommand));

        assertNotNull(resultTestUser);
        assertEquals(registeredUser, resultTestUser);
        verify(registerUserRepository).existsBy(Username.create(validUsernameString));
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

        when(registerUserRepository.existsBy(Username.create(validUsernameString))).thenReturn(false);
        when(registerUserRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0, User.class));

        User resultTestUser = assertDoesNotThrow(() -> registerUserUseCase.register(validUserCommand));

        var savedUserCaptor = ArgumentCaptor.forClass(User.class);
        verify(registerUserRepository).save(savedUserCaptor.capture());
        User savedUser = savedUserCaptor.getValue();

        assertNotNull(savedUser.encodedPassword());
        assertEquals(hashedPassword.value(), savedUser.encodedPassword().value());
        assertNotEquals(validRawPasswordString, savedUser.encodedPassword().value());
        assertEquals(hashedPassword.value(), resultTestUser.encodedPassword().value());
    }

    @Test
    public void IH001ShouldCreateUserWithActiveStatus() {
        var registerUserUseCase = new RegisterUserUseCase(passwordHasher, registerUserRepository, fixedClock);

        when(registerUserRepository.existsBy(Username.create(validUsernameString))).thenReturn(false);
        when(registerUserRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0, User.class));

        User resultTestUser = assertDoesNotThrow(() -> registerUserUseCase.register(validUserCommand));

        var savedUserCaptor = ArgumentCaptor.forClass(User.class);
        verify(registerUserRepository).save(savedUserCaptor.capture());
        User savedUser = savedUserCaptor.getValue();

        assertEquals(UserStatus.ACTIVE, savedUser.status());
        assertEquals(UserStatus.ACTIVE, resultTestUser.status());
    }
}
