package br.dev.andrestamatto.identityhub.bootstrap.config;

import br.dev.andrestamatto.identityhub.access.application.MembershipProjectionResult;
import br.dev.andrestamatto.identityhub.access.application.ProcessMembershipProjection;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.scheduling.annotation.Scheduled;

final class MembershipProjectionScheduler implements ApplicationListener<ContextClosedEvent> {

    static final String METRIC_NAME = "identityhub.membership.projection.cycles";
    private static final Logger LOGGER = LoggerFactory.getLogger(MembershipProjectionScheduler.class);

    private final ProcessMembershipProjection processor;
    private final MeterRegistry registry;
    private final UUID workerId;
    private final AtomicBoolean running = new AtomicBoolean(true);

    MembershipProjectionScheduler(
            ProcessMembershipProjection processor,
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
                LOGGER.warn("Membership projection cycle failed");
            }
        }
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        running.set(false);
    }

    private void record(MembershipProjectionResult result) {
        registry.counter(METRIC_NAME, "outcome", result.name().toLowerCase()).increment();
    }
}
