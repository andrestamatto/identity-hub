package br.dev.andrestamatto.identityhub.access.adapter.in.http;

import br.dev.andrestamatto.identityhub.access.application.MembershipGrantConflictException;
import br.dev.andrestamatto.identityhub.access.application.MembershipGrantResult;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
final class MembershipGrantMetrics {

    static final String METRIC_NAME = "identityhub.membership.grant.duration";

    private final MeterRegistry registry;

    MembershipGrantMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    MembershipGrantResult record(Supplier<MembershipGrantResult> action) {
        var sample = Timer.start(registry);
        try {
            var result = action.get();
            stop(sample, "accepted");
            return result;
        } catch (MembershipGrantConflictException exception) {
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
