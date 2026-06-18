package br.dev.andrestamatto.identityhub.application.usecase;

import br.dev.andrestamatto.identityhub.application.events.UserRegisteredPendingVerificationEvent;
import br.dev.andrestamatto.identityhub.application.exceptions.UserAlreadyExistsException;
import br.dev.andrestamatto.identityhub.application.ports.input.command.RegisterUserCommand;
import br.dev.andrestamatto.identityhub.application.ports.output.*;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.notifiers.UserNotifier;
import br.dev.andrestamatto.identityhub.domain.entities.User;
import br.dev.andrestamatto.identityhub.domain.valueobjects.*;
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

    private RegisterUser registerUserUseCase;
    private UserRepository registerUserRepository;
    private UserNotifier userNotifier;
    private DomainEventPublisher domainEventPublisher;
    private UserRegistrationPolicy userRegistrationPolicy;
    private RegisterUserCommand validUserCommand;
    private EncodedPassword hashedPassword;
    private VerificationTokenGenerator verificationTokenGenerator;
    private VerificationToken generatedVerificationToken;
    private UsernameResolver usernameResolver;

    @BeforeEach
    public void setup() {

        PasswordHasher passwordHasher = mock(PasswordHasher.class);
        userNotifier = mock(UserNotifier.class);
        domainEventPublisher = mock(DomainEventPublisher.class);
        userRegistrationPolicy = mock(UserRegistrationPolicy.class);
        registerUserRepository = mock(UserRepository.class);
        verificationTokenGenerator = mock(VerificationTokenGenerator.class);
        usernameResolver = mock(UsernameResolver.class);

        Clock fixedClock = Clock.fixed(Instant.parse("2026-05-18T10:00:00Z"), ZoneOffset.UTC);

        validUserCommand = new RegisterUserCommand(
                validUsernameString,
                validRawPasswordString
        );
        hashedPassword = new EncodedPassword(UserTestData.validEncodedPasswordString);
        generatedVerificationToken = new VerificationToken(UserTestData.validVerificationCode, NotificationMethod.EMAIL, Instant.parse("2099-01-01T00:15:00Z"));


        var validRawPassword = RawPassword.create(validRawPasswordString);

        when(passwordHasher.hashRawPassword(validRawPassword)).thenReturn(hashedPassword);
        when(usernameResolver.resolve(validUsernameString)).thenReturn(Username.create(validUsernameString));

        registerUserUseCase = new RegisterUserUseCase(
                userRegistrationPolicy,
                domainEventPublisher,
                passwordHasher,
                registerUserRepository,
                fixedClock,
                verificationTokenGenerator,
                usernameResolver
        );
    }

    @Test
    public void IH001ShouldExecuteUserWhenUsernameIsAvailable() {
        User registeredUser = UserTestData.createUser(UserTestData.registered(), UserStatus.ACTIVE);

        when(registerUserRepository.existsBy(Username.create(validUsernameString))).thenReturn(false);
        when(registerUserRepository.save(any(User.class))).thenReturn(registeredUser);
        when(userRegistrationPolicy.initialStatusFor(any(UsernameType.class))).thenReturn(UserStatus.ACTIVE);

        User resultTestUser = assertDoesNotThrow(() -> registerUserUseCase.execute(validUserCommand));

        assertNotNull(resultTestUser);
        assertEquals(registeredUser, resultTestUser);
        verify(registerUserRepository).existsBy(Username.create(validUsernameString));
        verify(registerUserRepository).save(any(User.class));
    }

    @Test
    public void IH001ShouldRejectWhenUsernameAlreadyExists() {
        when(registerUserRepository.existsBy(any())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> registerUserUseCase.execute(validUserCommand));
        verify(registerUserRepository, never()).save(any(User.class));
    }

    @Test
    public void IH001ShouldStoreOnlyEncodedPassword() {

        when(registerUserRepository.existsBy(Username.create(validUsernameString))).thenReturn(false);
        when(registerUserRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0, User.class));
        when(userRegistrationPolicy.initialStatusFor(any(UsernameType.class))).thenReturn(UserStatus.ACTIVE);

        User resultTestUser = assertDoesNotThrow(() -> registerUserUseCase.execute(validUserCommand));

        var savedUserCaptor = ArgumentCaptor.forClass(User.class);
        verify(registerUserRepository).save(savedUserCaptor.capture());
        User savedUser = savedUserCaptor.getValue();

        assertNotNull(savedUser.encodedPassword());
        assertEquals(hashedPassword.value(), savedUser.encodedPassword().value());
        assertNotEquals(validRawPasswordString, savedUser.encodedPassword().value());
        assertEquals(hashedPassword.value(), resultTestUser.encodedPassword().value());
    }

    @Test
    public void IH001ShouldCreateUserWithActiveStatusWhenUsernameTypeVerificationIsDisabled() {
        executeShouldCreateUserWithStatus(UserStatus.ACTIVE);
    }

    @Test
    public void IH001ShouldCreateUserWithPendingVerificationStatusWhenUsernameTypeVerificationIsEnabled() {
        executeShouldCreateUserWithStatus(UserStatus.PENDING_VERIFICATION);
    }

    @Test
    public void IH002ShouldSendConfirmationCodeOnFirstUserAccessWhenPendingVerificationStatus() {
        executeShouldCreateUserWithStatus(UserStatus.PENDING_VERIFICATION);
        verify(domainEventPublisher).publish(any(UserRegisteredPendingVerificationEvent.class));
        verify(verificationTokenGenerator).generate(NotificationMethod.EMAIL);
    }

    @Test
    public void IH002ShouldGenerateSmsVerificationTokenWhenUsernameIsPhone() {
        var phoneUserCommand = new RegisterUserCommand(
                "11999998888",
                validRawPasswordString
        );

        when(registerUserRepository.existsBy(Username.create("11999998888"))).thenReturn(false);
        when(usernameResolver.resolve("11999998888")).thenReturn(Username.phone("+5511999998888"));
        when(registerUserRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0, User.class));
        when(userRegistrationPolicy.initialStatusFor(any(UsernameType.class))).thenReturn(UserStatus.PENDING_VERIFICATION);
        when(verificationTokenGenerator.generate(NotificationMethod.SMS)).thenReturn(
                new VerificationToken(UserTestData.validVerificationCode, NotificationMethod.SMS, Instant.parse("2099-01-01T00:15:00Z"))
        );

        var resultTestUser = assertDoesNotThrow(() -> registerUserUseCase.execute(phoneUserCommand));

        assertEquals(UsernameType.PHONE, resultTestUser.username().usernameType());
        assertEquals("+5511999998888", resultTestUser.username().value());
        verify(userRegistrationPolicy).initialStatusFor(UsernameType.PHONE);
        verify(verificationTokenGenerator).generate(NotificationMethod.SMS);
        verify(domainEventPublisher).publish(any(UserRegisteredPendingVerificationEvent.class));
    }

    @Test
    public void IH002ShouldNotSendConfirmationCodeOnFirstUserAccessWhenActiveStatus() {
        executeShouldCreateUserWithStatus(UserStatus.ACTIVE);
        verify(domainEventPublisher, never()).publish(any());
        verify(verificationTokenGenerator, never()).generate(any(NotificationMethod.class));
    }

    private void executeShouldCreateUserWithStatus(UserStatus userStatus) {

        when(registerUserRepository.existsBy(Username.create(validUsernameString))).thenReturn(false);
        when(registerUserRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0, User.class));
        when(userRegistrationPolicy.initialStatusFor(any(UsernameType.class))).thenReturn(userStatus);
        when(verificationTokenGenerator.generate(any(NotificationMethod.class))).thenReturn(generatedVerificationToken);

        User resultTestUser = assertDoesNotThrow(() -> registerUserUseCase.execute(validUserCommand));

        var savedUserCaptor = ArgumentCaptor.forClass(User.class);
        verify(registerUserRepository).save(savedUserCaptor.capture());
        User savedUser = savedUserCaptor.getValue();

        assertEquals(userStatus, savedUser.status());
        assertEquals(userStatus, resultTestUser.status());
        verify(userRegistrationPolicy).initialStatusFor(UsernameType.EMAIL);
    }

}
