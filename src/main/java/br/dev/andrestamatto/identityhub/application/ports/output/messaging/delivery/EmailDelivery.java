package br.dev.andrestamatto.identityhub.application.ports.output.messaging.delivery;

import br.dev.andrestamatto.identityhub.application.ports.output.messaging.renderers.email.RenderedEmail;

/**
 * Low-level email delivery port for an already rendered email.
 * Implementations represent provider/transport choices such as SMTP, SES, or Resend.
 */
public interface EmailDelivery {
    void deliver(RenderedEmail email);
}
