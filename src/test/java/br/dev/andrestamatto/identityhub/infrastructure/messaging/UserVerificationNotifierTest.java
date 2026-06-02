package br.dev.andrestamatto.identityhub.infrastructure.messaging;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.EmailSender;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.SmsSender;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.MessageTemplate;
import br.dev.andrestamatto.identityhub.domain.valueobjects.NotificationMethod;
import br.dev.andrestamatto.identityhub.support.UserNotifierTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class UserVerificationNotifierTest {

    private EmailSender mockedEmailSender;
    private SmsSender mockedSmsSender;
    private UserVerificationNotifier userVerificationNotifier;

    @BeforeEach
    public void setUp() {
        mockedSmsSender = mock(SmsSender.class);
        mockedEmailSender = mock(EmailSender.class);
        userVerificationNotifier = new UserVerificationNotifier(
                mockedEmailSender,
                mockedSmsSender
        );
    }

    @Test
    public void IH002ShouldSendEmailWhenValidDataAndMethodIsEMAIL(){
        var notificationMessage = NotificationMessage.create(
                MessageTemplate.USER_VERIFICATION_CODE,
                UserNotifierTestData.validWhoEmail,
                Map.of("subject", UserNotifierTestData.verifyYourIdentitySubject, "message", UserNotifierTestData.validWhat)
        );

        assertDoesNotThrow(
                () -> userVerificationNotifier.notify(
                    notificationMessage,
                    NotificationMethod.EMAIL
                )
        );

        verify(mockedEmailSender).send(any(NotificationMessage.class));
        verify(mockedSmsSender, never()).send(any(NotificationMessage.class));

    }

    @Test
    public void IH002ShouldSendSMSWhenValidDataAndMethodIsSMS(){
        var notificationMessage = NotificationMessage.create(
                MessageTemplate.USER_VERIFICATION_CODE,
                UserNotifierTestData.validWhoSms,
                Map.of("subject", UserNotifierTestData.verifyYourIdentitySubject, "message", UserNotifierTestData.validWhat)
        );

        assertDoesNotThrow(
                () -> userVerificationNotifier.notify(
                        notificationMessage,
                        NotificationMethod.SMS
                )
        );

        verify(mockedEmailSender, never()).send(any(NotificationMessage.class));
        verify(mockedSmsSender).send(any(NotificationMessage.class));
    }

    @Test
    public void IH002ShouldSendEmailAndSmsWhenValidDataAndMethodIsBOTH(){
        var notificationMessage = NotificationMessage.create(
                MessageTemplate.USER_VERIFICATION_CODE,
                UserNotifierTestData.validWhoBoth,
                Map.of("subject", UserNotifierTestData.verifyYourIdentitySubject, "message", UserNotifierTestData.validWhat)
        );

        assertDoesNotThrow(
                () -> userVerificationNotifier.notify(
                        notificationMessage,
                        NotificationMethod.BOTH
                )
        );

        verify(mockedEmailSender).send(any(NotificationMessage.class));
        verify(mockedSmsSender).send(any(NotificationMessage.class));
    }

}
