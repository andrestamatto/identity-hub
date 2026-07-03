package br.dev.andrestamatto.identityhub.infrastructure.messaging;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.NotificationMessage;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.channels.NotificationChannels;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.senders.EmailSender;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.senders.SmsSender;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.senders.WhatsappSender;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.EmailMessageTemplate;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.MessageTemplates;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.SmsMessageTemplate;
import br.dev.andrestamatto.identityhub.application.ports.output.messaging.templates.WhatsappMessageTemplate;
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
    private WhatsappSender mockedWhatsappSender;
    private UserVerificationNotifier userVerificationNotifier;

    @BeforeEach
    public void setUp() {
        mockedSmsSender = mock(SmsSender.class);
        mockedEmailSender = mock(EmailSender.class);
        mockedWhatsappSender = mock(WhatsappSender.class);
        userVerificationNotifier = new UserVerificationNotifier(
                mockedEmailSender,
                mockedSmsSender,
                mockedWhatsappSender
        );
    }

    @Test
    public void IH002ShouldSendEmailWhenValidDataAndMethodIsEMAIL(){
        var notificationMessage = NotificationMessage.create(
                UserNotifierTestData.validWhoEmail,
                Map.of("subject", UserNotifierTestData.verifyYourIdentitySubject, "message", UserNotifierTestData.validWhat),
                verificationCodeTemplates(),
                NotificationChannels.email()
        );

        assertDoesNotThrow(
                () -> userVerificationNotifier.notify(
                    notificationMessage,
                    NotificationMethod.EMAIL
                )
        );

        verify(mockedEmailSender).send(any(NotificationMessage.class));
        verify(mockedSmsSender, never()).send(any(NotificationMessage.class));
        verify(mockedWhatsappSender, never()).send(any(NotificationMessage.class));

    }

    @Test
    public void IH002ShouldSendSMSWhenValidDataAndMethodIsSMS(){
        var notificationMessage = NotificationMessage.create(
                UserNotifierTestData.validWhoSms,
                Map.of("subject", UserNotifierTestData.verifyYourIdentitySubject, "message", UserNotifierTestData.validWhat),
                verificationCodeTemplates(),
                NotificationChannels.sms()
        );

        assertDoesNotThrow(
                () -> userVerificationNotifier.notify(
                        notificationMessage,
                        NotificationMethod.SMS
                )
        );

        verify(mockedEmailSender, never()).send(any(NotificationMessage.class));
        verify(mockedSmsSender).send(any(NotificationMessage.class));
        verify(mockedWhatsappSender, never()).send(any(NotificationMessage.class));
    }

    @Test
    public void IH002ShouldSendEmailAndSmsWhenValidDataAndMethodIsBOTH(){
        var notificationMessage = NotificationMessage.create(
                UserNotifierTestData.validWhoBoth,
                Map.of("subject", UserNotifierTestData.verifyYourIdentitySubject, "message", UserNotifierTestData.validWhat),
                verificationCodeTemplates(),
                NotificationChannels.all()
        );

        assertDoesNotThrow(
                () -> userVerificationNotifier.notify(
                        notificationMessage,
                        NotificationMethod.ALL
                )
        );

        verify(mockedEmailSender).send(any(NotificationMessage.class));
        verify(mockedSmsSender).send(any(NotificationMessage.class));
        verify(mockedWhatsappSender, never()).send(any(NotificationMessage.class));
    }

    @Test
    public void IH002ShouldSendWhatsappWhenValidDataAndMethodIsWhatsapp(){
        var notificationMessage = NotificationMessage.create(
                UserNotifierTestData.validWhoSms,
                Map.of("subject", UserNotifierTestData.verifyYourIdentitySubject, "message", UserNotifierTestData.validWhat),
                verificationCodeTemplates(),
                NotificationChannels.whatsapp()
        );

        assertDoesNotThrow(
                () -> userVerificationNotifier.notify(
                        notificationMessage,
                        NotificationMethod.WHATSAPP
                )
        );

        verify(mockedEmailSender, never()).send(any(NotificationMessage.class));
        verify(mockedSmsSender, never()).send(any(NotificationMessage.class));
        verify(mockedWhatsappSender).send(any(NotificationMessage.class));
    }

    private MessageTemplates verificationCodeTemplates() {
        return new MessageTemplates(
                EmailMessageTemplate.EMAIL_USER_VERIFICATION_CODE,
                SmsMessageTemplate.SMS_USER_VERIFICATION_CODE,
                WhatsappMessageTemplate.WHATSAPP_USER_VERIFICATION_CODE
        );
    }

}
