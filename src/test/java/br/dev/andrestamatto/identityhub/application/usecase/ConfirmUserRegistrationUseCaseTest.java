package br.dev.andrestamatto.identityhub.application.usecase;


import br.dev.andrestamatto.identityhub.application.events.UserConfirmedEvent;
import br.dev.andrestamatto.identityhub.application.exceptions.UserNotFoundException;
import br.dev.andrestamatto.identityhub.application.exceptions.UserStatusDoesNotMatchRegistrationConfirmationException;
import br.dev.andrestamatto.identityhub.application.exceptions.VerificationTokenException;
import br.dev.andrestamatto.identityhub.application.ports.input.command.ConfirmUserCommand;
import br.dev.andrestamatto.identityhub.application.ports.output.UserRepository;
import br.dev.andrestamatto.identityhub.domain.entities.User;
import br.dev.andrestamatto.identityhub.domain.valueobjects.NotificationMethod;
import br.dev.andrestamatto.identityhub.domain.valueobjects.UserStatus;
import br.dev.andrestamatto.identityhub.domain.valueobjects.Username;
import br.dev.andrestamatto.identityhub.support.UserTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConfirmUserRegistrationUseCaseTest {

    private ConfirmUser confirmUserUseCase;
    private ConfirmUser expiredConfirmUserUseCase;
    private UserRepository mockedUserRepository;
    private ApplicationEventPublisher mockedAppEventPublisher;
    private ConfirmUserCommand validConfirmUserCommand;
    private ConfirmUserCommand invalidConfirmUserCommand;
    private Clock baseClock;

    @BeforeEach
    public void setup() {
        mockedUserRepository = mock(UserRepository.class);
        mockedAppEventPublisher = mock(ApplicationEventPublisher.class);
        validConfirmUserCommand = new ConfirmUserCommand(
                UserTestData.validUsernameString,
                UserTestData.validVerificationCode
        );

        invalidConfirmUserCommand = new ConfirmUserCommand(
                UserTestData.validUsernameString,
                UserTestData.invalidVerificationCode
        );

        var baseInstant = Instant.parse("2099-05-28T10:00:00Z");
        baseClock = Clock.fixed(baseInstant, ZoneOffset.UTC);

        confirmUserUseCase = new ConfirmUserUseCase(
              mockedUserRepository,
              mockedAppEventPublisher,
              baseClock
        );

        expiredConfirmUserUseCase = new ConfirmUserUseCase(
                mockedUserRepository,
                mockedAppEventPublisher,
                // simulates a code verification 30 minutes in the future.
                Clock.fixed(baseInstant.plusSeconds(1800), ZoneOffset.UTC)
        );

    }

    @Test
    public void IH002ShouldActivateUserAndPublishUserConfirmedEventGivenValidVerificationCode() {

        createDefaultExecuteConfirmUserUseCaseScenario();

        assertDoesNotThrow(() -> confirmUserUseCase.execute(validConfirmUserCommand));

        var savedUserCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(mockedUserRepository).save(savedUserCaptor.capture());
        var savedUser = savedUserCaptor.getValue();
        assertEquals(UserStatus.ACTIVE, savedUser.status());
        assertNull(savedUser.verificationToken());

        var eventCaptor = org.mockito.ArgumentCaptor.forClass(UserConfirmedEvent.class);
        verify(mockedAppEventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(UserTestData.validUsernameString, eventCaptor.getValue().username().value());
    }

    @Test
    public void IH002ShouldRejectConfirmationWhenVerificationCodeIsInvalid() {
        createDefaultExecuteConfirmUserUseCaseScenario();

        assertThrows(VerificationTokenException.class, () -> confirmUserUseCase.execute(invalidConfirmUserCommand));

        verify(mockedUserRepository, never()).save(any(User.class));
        verify(mockedAppEventPublisher, never()).publishEvent(any(UserConfirmedEvent.class));
    }

    @Test
    public void IH002ShouldRejectConfirmationWhenVerificationTokenIsExpired() {
        createDefaultExecuteConfirmUserUseCaseScenario();

        assertThrows(VerificationTokenException.class, () -> expiredConfirmUserUseCase.execute(validConfirmUserCommand));

        verify(mockedUserRepository, never()).save(any(User.class));
        verify(mockedAppEventPublisher, never()).publishEvent(any(UserConfirmedEvent.class));
    }

    @Test
    public void IH002ShouldRejectConfirmationWhenUserIsNotPendingVerification() {
        var activeUser = UserTestData.createUser(UserTestData.registered(), UserStatus.ACTIVE);
        var lockedUser = UserTestData.createUser(UserTestData.registered(), UserStatus.LOCKED);
        var disabledUser = UserTestData.createUser(UserTestData.registered(), UserStatus.DISABLED);

        shouldRejectConfirmationWhenInvalidUserStatus(activeUser);
        shouldRejectConfirmationWhenInvalidUserStatus(lockedUser);
        shouldRejectConfirmationWhenInvalidUserStatus(disabledUser);

    }

    @Test
    public void IH002ShouldRejectConfirmationWhenUserDoesNotExist() {
        when(mockedUserRepository.findByUsername(any(Username.class))).thenReturn(null);

        assertThrows(UserNotFoundException.class, () -> confirmUserUseCase.execute(validConfirmUserCommand));

        verify(mockedUserRepository, never()).save(any(User.class));
        verify(mockedAppEventPublisher, never()).publishEvent(any(UserConfirmedEvent.class));
    }

    private void createDefaultExecuteConfirmUserUseCaseScenario() {
        var validToken = UserTestData.createDefaultValidVerificationToken(
                UserTestData.validVerificationCode,
                baseClock
        );
        var pendingConfirmationUser = UserTestData.createUser(
                UserTestData.registeredWithVerificationToken(validToken),
                UserStatus.PENDING_VERIFICATION
        );
        var activateUser = UserTestData.activate(pendingConfirmationUser);

        when(mockedUserRepository.findByUsername(any(Username.class))).thenReturn(pendingConfirmationUser);
        when(mockedUserRepository.save(any(User.class))).thenReturn(activateUser);
    }

    private void shouldRejectConfirmationWhenInvalidUserStatus(User userWithInvalidUserStatus) {
        when(mockedUserRepository.findByUsername(any(Username.class))).thenReturn(userWithInvalidUserStatus);

        assertThrows(UserStatusDoesNotMatchRegistrationConfirmationException.class, () -> confirmUserUseCase.execute(validConfirmUserCommand));

        verify(mockedUserRepository, never()).save(any(User.class));
        verify(mockedAppEventPublisher, never()).publishEvent(any(UserConfirmedEvent.class));

    }

}
