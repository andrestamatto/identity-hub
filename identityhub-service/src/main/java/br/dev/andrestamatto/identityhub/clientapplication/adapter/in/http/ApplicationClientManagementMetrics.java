package br.dev.andrestamatto.identityhub.clientapplication.adapter.in.http;

import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientConfigurationResult;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientSnapshot;
import br.dev.andrestamatto.identityhub.clientapplication.application.ClientApplicationConflictException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
final class ApplicationClientManagementMetrics {

    static final String CONFIGURATION_METRIC =
            "identityhub.application_client.configuration.duration";
    static final String RECONCILIATION_METRIC =
            "identityhub.application_client.reconciliation.duration";

    private final MeterRegistry registry;

    ApplicationClientManagementMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    ApplicationClientConfigurationResult recordConfiguration(
            Supplier<ApplicationClientConfigurationResult> action) {
        var sample = Timer.start(registry);
        try {
            var result = action.get();
            stop(sample, CONFIGURATION_METRIC, result.created() ? "created" : "replayed");
            return result;
        } catch (ClientApplicationConflictException exception) {
            stop(sample, CONFIGURATION_METRIC, "conflict");
            throw exception;
        } catch (IllegalArgumentException exception) {
            stop(sample, CONFIGURATION_METRIC, "invalid");
            throw exception;
        } catch (RuntimeException exception) {
            stop(sample, CONFIGURATION_METRIC, "failure");
            throw exception;
        }
    }

    ApplicationClientSnapshot recordReconciliation(Supplier<ApplicationClientSnapshot> action) {
        var sample = Timer.start(registry);
        try {
            var result = action.get();
            stop(sample, RECONCILIATION_METRIC, "accepted");
            return result;
        } catch (RuntimeException exception) {
            stop(sample, RECONCILIATION_METRIC, "failure");
            throw exception;
        }
    }

    private void stop(Timer.Sample sample, String metric, String outcome) {
        sample.stop(Timer.builder(metric)
                .tag("outcome", outcome)
                .register(registry));
    }
}
