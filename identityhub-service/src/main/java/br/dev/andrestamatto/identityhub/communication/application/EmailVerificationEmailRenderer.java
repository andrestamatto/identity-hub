package br.dev.andrestamatto.identityhub.communication.application;

public final class EmailVerificationEmailRenderer {

    public OutboundEmail render(EmailDelivery delivery) {
        if (delivery.purpose() != EmailDeliveryPurpose.EMAIL_VERIFICATION
                || delivery.sensitiveContent() == null) {
            throw new IllegalArgumentException("Email verification delivery is invalid");
        }
        var subject = "[%s] Verify your email".formatted(delivery.applicationDisplayName());
        var body = """
                Verify your email for %s by opening the official link below:

                %s

                This link expires in 30 minutes and can be used only once.
                If you did not request this registration, ignore this message.
                """.formatted(
                        delivery.applicationDisplayName(),
                        delivery.sensitiveContent());
        return new OutboundEmail(delivery.recipient(), subject, body);
    }
}
