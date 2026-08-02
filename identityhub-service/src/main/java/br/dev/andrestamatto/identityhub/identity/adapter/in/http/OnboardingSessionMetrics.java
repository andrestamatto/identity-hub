package br.dev.andrestamatto.identityhub.identity.adapter.in.http;

import br.dev.andrestamatto.identityhub.clientapplication.application.OnboardingOriginRejectedException;
import br.dev.andrestamatto.identityhub.identity.application.BeginOnboardingSession;
import br.dev.andrestamatto.identityhub.identity.application.OnboardingSessionConflictException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
final class OnboardingSessionMetrics {

    static final String METRIC_NAME = "identityhub.onboarding_session.initiation.duration";

    private final MeterRegistry registry;

    OnboardingSessionMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    BeginOnboardingSession.Result record(Supplier<BeginOnboardingSession.Result> action) {
        var sample = Timer.start(registry);
        try {
            var result = action.get();
            stop(sample, result.created() ? "created" : "replayed");
            return result;
        } catch (OnboardingSessionConflictException exception) {
            stop(sample, "conflict");
            throw exception;
        } catch (OnboardingOriginRejectedException exception) {
            stop(sample, "rejected");
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
