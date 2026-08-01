package br.dev.andrestamatto.identityhub.communication.adapter.out.smtp;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;

import br.dev.andrestamatto.identityhub.communication.application.EmailDeliveryException;
import br.dev.andrestamatto.identityhub.communication.application.EmailDeliveryFailureCode;
import br.dev.andrestamatto.identityhub.communication.application.OutboundEmail;
import br.dev.andrestamatto.identityhub.communication.domain.EmailRecipient;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class SmtpEmailDeliverySenderTest {

    private static final OutboundEmail EMAIL = new OutboundEmail(
            new EmailRecipient("andre@example.com"), "Subject", "Body");

    @Test
    void sendsPlainTextMessageWithConfiguredSender() {
        var mailSender = mock(JavaMailSender.class);
        var sender = new SmtpEmailDeliverySender(mailSender, "identity@example.com");

        sender.send(EMAIL);

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void classifiesProviderAvailabilityAsRetryable() {
        var mailSender = mock(JavaMailSender.class);
        doThrow(new MailSendException("synthetic"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> new SmtpEmailDeliverySender(
                        mailSender, "identity@example.com").send(EMAIL))
                .isInstanceOfSatisfying(EmailDeliveryException.class, exception -> {
                    org.assertj.core.api.Assertions.assertThat(exception.retryable()).isTrue();
                    org.assertj.core.api.Assertions.assertThat(exception.failureCode())
                            .isEqualTo(EmailDeliveryFailureCode.PROVIDER_UNAVAILABLE);
                });
    }

    @Test
    void classifiesAuthenticationFailureAsPermanent() {
        var mailSender = mock(JavaMailSender.class);
        doThrow(new MailAuthenticationException("synthetic"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> new SmtpEmailDeliverySender(
                        mailSender, "identity@example.com").send(EMAIL))
                .isInstanceOfSatisfying(EmailDeliveryException.class, exception -> {
                    org.assertj.core.api.Assertions.assertThat(exception.retryable()).isFalse();
                    org.assertj.core.api.Assertions.assertThat(exception.failureCode())
                            .isEqualTo(EmailDeliveryFailureCode.PROVIDER_AUTHENTICATION_FAILED);
                });
    }
}
