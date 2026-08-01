package br.dev.andrestamatto.identityhub.communication.application;

import java.util.Objects;

public final class EmailDeliveryRenderer {

    private final PasswordChangedEmailRenderer passwordChanged;
    private final EmailVerificationEmailRenderer emailVerification;

    public EmailDeliveryRenderer(
            PasswordChangedEmailRenderer passwordChanged,
            EmailVerificationEmailRenderer emailVerification) {
        this.passwordChanged = Objects.requireNonNull(passwordChanged);
        this.emailVerification = Objects.requireNonNull(emailVerification);
    }

    public OutboundEmail render(EmailDelivery delivery) {
        return switch (delivery.purpose()) {
            case PASSWORD_CHANGED -> passwordChanged.render(delivery);
            case EMAIL_VERIFICATION -> emailVerification.render(delivery);
        };
    }
}
