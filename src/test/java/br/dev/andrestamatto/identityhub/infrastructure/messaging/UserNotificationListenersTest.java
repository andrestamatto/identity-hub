package br.dev.andrestamatto.identityhub.infrastructure.messaging;

import br.dev.andrestamatto.identityhub.application.events.UserConfirmedEvent;
import br.dev.andrestamatto.identityhub.application.events.UserRegisteredPendingVerificationEvent;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.channels.NotificationChannel;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.notifiers.UserNotifier;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.EmailMessageTemplate;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.SmsMessageTemplate;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.WhatsappMessageTemplate;
import br.dev.andrestamatto.identityhub.domain.valueobjects.NotificationMethod;
import br.dev.andrestamatto.identityhub.domain.valueobjects.Username;
import br.dev.andrestamatto.identityhub.domain.valueobjects.UsernameType;
import br.dev.andrestamatto.identityhub.domain.valueobjects.VerificationToken;
import br.dev.andrestamatto.identityhub.support.UserTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class UserNotificationListenersTest {

    private UserNotifier userNotifier;
    private UserNotificationListeners listeners;

    @BeforeEach
    public void setup() {
        userNotifier = mock(UserNotifier.class);
        listeners = new UserNotificationListeners(userNotifier);
    }

    @Test
    public void shouldNotifyByEmailWhenUserIsRegisteredPendingEmailVerification() {
        var token = new VerificationToken(
                UserTestData.validVerificationCode,
                NotificationMethod.EMAIL,
                Instant.parse("2026-06-09T13:15:00Z")
        );
        var event = new UserRegisteredPendingVerificationEvent(
                Username.create(UserTestData.validUsernameString),
                token
        );

        listeners.on(event);

        var messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        var methodCaptor = ArgumentCaptor.forClass(NotificationMethod.class);
        verify(userNotifier).notify(messageCaptor.capture(), methodCaptor.capture());

        var message = messageCaptor.getValue();
        assertEquals(NotificationMethod.EMAIL, methodCaptor.getValue());
        assertEquals(UserTestData.validUsernameString, message.recipient());
        assertEquals(UserTestData.validVerificationCode, message.details().get("verificationCode"));
        assertEquals(EmailMessageTemplate.EMAIL_USER_VERIFICATION_CODE, message.messageTemplates().emailMessageTemplate());
        assertEquals(SmsMessageTemplate.SMS_USER_VERIFICATION_CODE, message.messageTemplates().smsMessageTemplate());
        assertEquals(WhatsappMessageTemplate.WHATSAPP_USER_VERIFICATION_CODE, message.messageTemplates().whatsappMessageTemplate());
        assertTrue(message.notificationChannels().values().contains(NotificationChannel.EMAIL));
    }

    @Test
    public void shouldNotifyBySmsWhenUserIsRegisteredPendingPhoneVerification() {
        var token = new VerificationToken(
                UserTestData.validVerificationCode,
                NotificationMethod.SMS,
                Instant.parse("2026-06-09T13:15:00Z")
        );
        var event = new UserRegisteredPendingVerificationEvent(
                Username.create("+5511999998888", UsernameType.PHONE),
                token
        );

        listeners.on(event);

        var messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        var methodCaptor = ArgumentCaptor.forClass(NotificationMethod.class);
        verify(userNotifier).notify(messageCaptor.capture(), methodCaptor.capture());

        assertEquals(NotificationMethod.SMS, methodCaptor.getValue());
        assertTrue(messageCaptor.getValue().notificationChannels().values().contains(NotificationChannel.SMS));
    }

    @Test
    public void shouldNotifyByEmailAndSmsWhenUserIsRegisteredPendingBothVerificationMethods() {
        var token = new VerificationToken(
                UserTestData.validVerificationCode,
                NotificationMethod.ALL,
                Instant.parse("2026-06-09T13:15:00Z")
        );
        var event = new UserRegisteredPendingVerificationEvent(
                Username.create(UserTestData.validUsernameString),
                token
        );

        listeners.on(event);

        var messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        var methodCaptor = ArgumentCaptor.forClass(NotificationMethod.class);
        verify(userNotifier).notify(messageCaptor.capture(), methodCaptor.capture());

        var channels = messageCaptor.getValue().notificationChannels().values();
        assertEquals(NotificationMethod.ALL, methodCaptor.getValue());
        assertTrue(channels.contains(NotificationChannel.EMAIL));
        assertTrue(channels.contains(NotificationChannel.SMS));
    }

    @Test
    public void shouldNotifyByWhatsappWhenUserIsRegisteredPendingWhatsappVerification() {
        var token = new VerificationToken(
                UserTestData.validVerificationCode,
                NotificationMethod.WHATSAPP,
                Instant.parse("2026-06-09T13:15:00Z")
        );
        var event = new UserRegisteredPendingVerificationEvent(
                Username.phone("+5511999998888"),
                token
        );

        listeners.on(event);

        var messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        var methodCaptor = ArgumentCaptor.forClass(NotificationMethod.class);
        verify(userNotifier).notify(messageCaptor.capture(), methodCaptor.capture());

        var message = messageCaptor.getValue();
        assertEquals(NotificationMethod.WHATSAPP, methodCaptor.getValue());
        assertEquals("+5511999998888", message.recipient());
        assertEquals(WhatsappMessageTemplate.WHATSAPP_USER_VERIFICATION_CODE, message.messageTemplates().whatsappMessageTemplate());
        assertTrue(message.notificationChannels().values().contains(NotificationChannel.WHATSAPP));
    }

    @Test
    public void shouldNotifyUserConfirmedByEmail() {
        var event = new UserConfirmedEvent(Username.create(UserTestData.validUsernameString));

        listeners.on(event);

        var messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        var methodCaptor = ArgumentCaptor.forClass(NotificationMethod.class);
        verify(userNotifier).notify(messageCaptor.capture(), methodCaptor.capture());

        assertEquals(NotificationMethod.EMAIL, methodCaptor.getValue());
        assertEquals(EmailMessageTemplate.EMAIL_USER_SUCCESSFULLY_ACTIVATED, messageCaptor.getValue().messageTemplates().emailMessageTemplate());
        assertEquals(SmsMessageTemplate.SMS_USER_SUCCESSFULLY_ACTIVATED, messageCaptor.getValue().messageTemplates().smsMessageTemplate());
        assertTrue(messageCaptor.getValue().notificationChannels().values().contains(NotificationChannel.EMAIL));
    }

    @Test
    public void shouldNotifyUserConfirmedBySmsWhenUsernameIsPhone() {
        var event = new UserConfirmedEvent(Username.phone("+5511999998888"));

        listeners.on(event);

        var messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        var methodCaptor = ArgumentCaptor.forClass(NotificationMethod.class);
        verify(userNotifier).notify(messageCaptor.capture(), methodCaptor.capture());

        assertEquals(NotificationMethod.SMS, methodCaptor.getValue());
        assertEquals("+5511999998888", messageCaptor.getValue().recipient());
        assertTrue(messageCaptor.getValue().notificationChannels().values().contains(NotificationChannel.SMS));
    }

    @Test
    public void shouldNotPropagateNotificationFailuresFromAsyncListener() {
        var token = new VerificationToken(
                UserTestData.validVerificationCode,
                NotificationMethod.EMAIL,
                Instant.parse("2026-06-09T13:15:00Z")
        );
        var event = new UserRegisteredPendingVerificationEvent(
                Username.create(UserTestData.validUsernameString),
                token
        );

        doThrow(new RuntimeException("SMTP unavailable"))
                .when(userNotifier)
                .notify(any(NotificationMessage.class), any(NotificationMethod.class));

        assertDoesNotThrow(() -> listeners.on(event));
    }
}
