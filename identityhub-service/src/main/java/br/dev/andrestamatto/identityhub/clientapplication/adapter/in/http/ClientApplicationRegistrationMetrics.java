package br.dev.andrestamatto.identityhub.clientapplication.adapter.in.http;

import br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationConflictException;
import br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationRegistration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
final class ClientApplicationRegistrationMetrics {

    static final String METRIC_NAME = "identityhub.client_application.registration.duration";

    private final MeterRegistry registry;

    ClientApplicationRegistrationMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    ClientApplicationRegistration record(
            Supplier<ClientApplicationRegistration> registration) {
        var sample = Timer.start(registry);
        try {
            var result = registration.get();
            stop(sample, result.created() ? "created" : "replayed");
            return result;
        } catch (ClientApplicationConflictException exception) {
            stop(sample, "conflict");
            throw exception;
        } catch (IllegalArgumentException exception) {
            stop(sample, "invalid");
            throw exception;
        } catch (RuntimeException exception) {
            stop(sample, "failure");
            throw exception;
        }
    }

    private void stop(Timer.Sample sample, String outcome) {
        sample.stop(Timer.builder(METRIC_NAME)
                .tag("outcome", outcome)
                .register(registry));
    }
}
