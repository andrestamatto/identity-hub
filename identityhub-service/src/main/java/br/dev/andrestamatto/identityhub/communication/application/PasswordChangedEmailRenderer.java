package br.dev.andrestamatto.identityhub.communication.application;

public final class PasswordChangedEmailRenderer {

    public OutboundEmail render(EmailDelivery delivery) {
        var subject = "[%s] Password changed".formatted(delivery.applicationDisplayName());
        var body = """
                Your password for %s was changed.

                Environment: %s
                Application: %s

                If you did not make this change, contact the application support immediately.
                """.formatted(
                        delivery.applicationDisplayName(),
                        delivery.environment(),
                        delivery.applicationIdentifier());
        return new OutboundEmail(delivery.recipient(), subject, body);
    }
}
