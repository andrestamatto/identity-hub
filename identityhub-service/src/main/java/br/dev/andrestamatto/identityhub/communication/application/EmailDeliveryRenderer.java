package br.dev.andrestamatto.identityhub.communication.application;

import java.util.Objects;

public final class EmailDeliveryRenderer {

    private final PasswordChangedEmailRenderer passwordChanged;
    private final EmailVerificationEmailRenderer emailVerification;
    private final PasswordRecoveryEmailRenderer passwordRecovery;

    public EmailDeliveryRenderer(
            PasswordChangedEmailRenderer passwordChanged,
            EmailVerificationEmailRenderer emailVerification,
            PasswordRecoveryEmailRenderer passwordRecovery) {
        this.passwordChanged = Objects.requireNonNull(passwordChanged);
        this.emailVerification = Objects.requireNonNull(emailVerification);
        this.passwordRecovery = Objects.requireNonNull(passwordRecovery);
    }

    public OutboundEmail render(EmailDelivery delivery) {
        return switch (delivery.purpose()) {
            case PASSWORD_CHANGED -> passwordChanged.render(delivery);
            case EMAIL_VERIFICATION -> emailVerification.render(delivery);
            case PASSWORD_RECOVERY -> passwordRecovery.render(delivery);
        };
    }
}
