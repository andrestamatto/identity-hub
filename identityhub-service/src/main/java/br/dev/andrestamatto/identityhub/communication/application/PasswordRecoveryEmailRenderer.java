package br.dev.andrestamatto.identityhub.communication.application;

public final class PasswordRecoveryEmailRenderer {

    public OutboundEmail render(EmailDelivery delivery) {
        if (delivery.purpose() != EmailDeliveryPurpose.PASSWORD_RECOVERY
                || delivery.sensitiveContent() == null) {
            throw new IllegalArgumentException("Password recovery delivery is invalid");
        }
        var subject = "[%s] Reset your password".formatted(
                delivery.applicationDisplayName());
        var body = """
                Reset your IdentityHub password by opening the official link below:

                %s

                This link expires in 15 minutes and can be used only once.
                If you did not request this change, ignore this message.
                """.formatted(delivery.sensitiveContent());
        return new OutboundEmail(delivery.recipient(), subject, body);
    }
}
