package br.dev.andrestamatto.identityhub.clientapplication.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.dev.andrestamatto.identityhub.clientapplication.domain.ApplicationIdentifier;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplication;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplicationId;
import br.dev.andrestamatto.identityhub.clientapplication.domain.ClientApplicationState;
import br.dev.andrestamatto.identityhub.clientapplication.domain.DisplayName;
import br.dev.andrestamatto.identityhub.clientapplication.domain.SelfRegistrationPolicy;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetClientApplicationByIdentifierTest {

    private static final UUID APPLICATION_ID =
            UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0");

    @Test
    void resolvesApplicationByItsStableIdentifier() {
        var repository = mock(ClientApplicationRepository.class);
        var identifier = new ApplicationIdentifier("auto-radar");
        when(repository.findByIdentifier(identifier)).thenReturn(Optional.of(application(identifier)));

        var result = new GetClientApplicationByIdentifier(repository).execute("auto-radar");

        assertThat(result.applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(result.identifier()).isEqualTo("auto-radar");
    }

    @Test
    void hidesWhetherAnInvalidOrUnknownIdentifierWasUsed() {
        var repository = mock(ClientApplicationRepository.class);
        var useCase = new GetClientApplicationByIdentifier(repository);

        assertThatThrownBy(() -> useCase.execute("INVALID"))
                .isInstanceOf(ClientApplicationUnavailableException.class)
                .hasMessage("Client application is unavailable");
        assertThatThrownBy(() -> useCase.execute("unknown-app"))
                .isInstanceOf(ClientApplicationUnavailableException.class)
                .hasMessage("Client application is unavailable");
    }

    private ClientApplication application(ApplicationIdentifier identifier) {
        return ClientApplication.reconstitute(
                new ClientApplicationId(APPLICATION_ID),
                identifier,
                new DisplayName("Auto Radar"),
                ClientApplicationState.DRAFT,
                SelfRegistrationPolicy.ENABLED,
                Instant.parse("2026-08-01T10:00:00Z"));
    }
}
