package br.dev.andrestamatto.identityhub.communication.adapter.out.smtp;

import br.dev.andrestamatto.identityhub.communication.application.EmailDeliveryException;
import br.dev.andrestamatto.identityhub.communication.application.EmailDeliveryFailureCode;
import br.dev.andrestamatto.identityhub.communication.application.EmailDeliverySender;
import br.dev.andrestamatto.identityhub.communication.application.OutboundEmail;
import java.util.Objects;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public final class SmtpEmailDeliverySender implements EmailDeliverySender {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpEmailDeliverySender(JavaMailSender mailSender, String fromAddress) {
        this.mailSender = Objects.requireNonNull(mailSender);
        this.fromAddress = requireText(fromAddress);
    }

    @Override
    public void send(OutboundEmail email) {
        var message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email.recipient().value());
        message.setSubject(email.subject());
        message.setText(email.body());
        try {
            mailSender.send(message);
        } catch (MailAuthenticationException exception) {
            throw EmailDeliveryException.permanent(
                    EmailDeliveryFailureCode.PROVIDER_AUTHENTICATION_FAILED,
                    exception);
        } catch (MailParseException exception) {
            throw EmailDeliveryException.permanent(
                    EmailDeliveryFailureCode.INVALID_MESSAGE,
                    exception);
        } catch (MailSendException exception) {
            throw EmailDeliveryException.retryable(
                    EmailDeliveryFailureCode.PROVIDER_UNAVAILABLE,
                    exception);
        } catch (RuntimeException exception) {
            throw EmailDeliveryException.retryable(
                    EmailDeliveryFailureCode.UNEXPECTED_PROVIDER_FAILURE,
                    exception);
        }
    }

    private static String requireText(String value) {
        Objects.requireNonNull(value, "SMTP from address is required");
        var normalized = value.trim();
        if (normalized.isEmpty()
                || normalized.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("SMTP from address is invalid");
        }
        return normalized;
    }
}
