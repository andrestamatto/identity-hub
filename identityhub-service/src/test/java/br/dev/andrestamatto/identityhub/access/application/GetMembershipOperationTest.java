package br.dev.andrestamatto.identityhub.access.application;

import static org.assertj.core.api.Assertions.assertThat;

import br.dev.andrestamatto.identityhub.access.domain.MembershipApplicationRef;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetMembershipOperationTest {

    @Test
    void scopesLookupToTheAuthenticatedApplication() {
        var applicationId = UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0");
        var operationId = UUID.fromString("06d068e0-78d5-4df6-97dd-88df538ab948");
        var expected = new MembershipOperationStatus(
                operationId,
                UUID.fromString("c50638fe-0b91-4f47-81e6-2bd183040c1c"),
                "ACTIVE",
                "APPLIED",
                1,
                null,
                Instant.parse("2026-08-03T01:00:00Z"),
                Instant.parse("2026-08-03T01:00:01Z"));
        var repository = new MembershipGrantRepository() {
            @Override
            public MembershipGrantOperation addOrReplay(MembershipGrantOperation proposed) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Optional<MembershipOperationStatus> findStatus(
                    UUID requestedOperationId,
                    MembershipApplicationRef applicationRef) {
                assertThat(requestedOperationId).isEqualTo(operationId);
                assertThat(applicationRef.value()).isEqualTo(applicationId);
                return Optional.of(expected);
            }
        };

        assertThat(new GetMembershipOperation(repository).execute(operationId, applicationId))
                .contains(expected);
    }
}
