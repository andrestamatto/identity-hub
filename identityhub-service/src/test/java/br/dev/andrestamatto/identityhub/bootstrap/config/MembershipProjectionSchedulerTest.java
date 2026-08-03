package br.dev.andrestamatto.identityhub.bootstrap.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.dev.andrestamatto.identityhub.access.application.MembershipProjectionResult;
import br.dev.andrestamatto.identityhub.access.application.ProcessMembershipProjection;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MembershipProjectionSchedulerTest {

    @Test
    void recordsOnlyStableOutcomeForCycleAndDuration() {
        var processor = mock(ProcessMembershipProjection.class);
        var registry = new SimpleMeterRegistry();
        var workerId = UUID.fromString("2e993831-1468-44f7-958d-b82a80a784bb");
        when(processor.processNext(workerId)).thenReturn(MembershipProjectionResult.APPLIED);
        var scheduler = new MembershipProjectionScheduler(processor, registry, workerId);

        scheduler.processNext();

        assertThat(registry.get(MembershipProjectionScheduler.METRIC_NAME)
                .tag("outcome", "applied").counter().count()).isOne();
        assertThat(registry.get(MembershipProjectionScheduler.DURATION_METRIC_NAME)
                .tag("outcome", "applied").timer().count()).isOne();
    }
}
