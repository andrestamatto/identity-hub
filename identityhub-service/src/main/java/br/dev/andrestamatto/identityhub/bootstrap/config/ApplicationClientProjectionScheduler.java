package br.dev.andrestamatto.identityhub.bootstrap.config;

import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientProjectionResult;
import br.dev.andrestamatto.identityhub.clientapplication.application.ProcessApplicationClientProjection;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.scheduling.annotation.Scheduled;

final class ApplicationClientProjectionScheduler
        implements ApplicationListener<ContextClosedEvent> {

    static final String METRIC_NAME = "identityhub.application_client.projection.cycles";

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ApplicationClientProjectionScheduler.class);

    private final ProcessApplicationClientProjection processor;
    private final MeterRegistry registry;
    private final UUID workerId;
    private final AtomicBoolean running = new AtomicBoolean(true);

    ApplicationClientProjectionScheduler(
            ProcessApplicationClientProjection processor,
            MeterRegistry registry,
            UUID workerId) {
        this.processor = Objects.requireNonNull(processor);
        this.registry = Objects.requireNonNull(registry);
        this.workerId = Objects.requireNonNull(workerId);
    }

    @Scheduled(
            fixedDelayString = "${identityhub.keycloak.management.poll-interval}",
            initialDelayString = "${identityhub.keycloak.management.poll-interval}")
    void processNext() {
        if (!running.get()) {
            return;
        }
        try {
            record(processor.processNext(workerId));
        } catch (RuntimeException exception) {
            if (running.get()) {
                registry.counter(METRIC_NAME, "outcome", "cycle_failure").increment();
                LOGGER.warn("Application client projection cycle failed");
            }
        }
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        running.set(false);
    }

    private void record(ApplicationClientProjectionResult result) {
        registry.counter(METRIC_NAME, "outcome", result.name().toLowerCase())
                .increment();
    }
}
