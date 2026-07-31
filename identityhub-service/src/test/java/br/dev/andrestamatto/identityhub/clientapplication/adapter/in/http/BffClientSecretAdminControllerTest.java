package br.dev.andrestamatto.identityhub.clientapplication.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.dev.andrestamatto.identityhub.clientapplication.application.ConfidentialClientSecret;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientProjectionException;
import br.dev.andrestamatto.identityhub.clientapplication.application.ApplicationClientProjectionFailureCode;
import br.dev.andrestamatto.identityhub.clientapplication.application.RotateBffClientSecret;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

class BffClientSecretAdminControllerTest {

    @Test
    void returnsRotatedSecretOnceWithNonCacheableHeaders() {
        var applicationId = UUID.fromString("184b5f54-1c97-4ea0-a6d7-8bad8f6d8ff0");
        var clientId = UUID.fromString("72c43df3-9f34-4dc6-85cc-5d323762f299");
        var useCase = mock(RotateBffClientSecret.class);
        when(useCase.execute(applicationId, clientId))
                .thenReturn(new ConfidentialClientSecret("one-time-secret"));
        var controller = new BffClientSecretAdminController(useCase);

        var response = controller.rotate(applicationId, clientId);

        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(response.getHeaders().getFirst(HttpHeaders.PRAGMA)).isEqualTo("no-cache");
        assertThat(response.getBody().clientSecret()).isEqualTo("one-time-secret");
        assertThat(response.getBody().toString()).doesNotContain("one-time-secret");
    }

    @Test
    void sanitizesRetryableCredentialProviderFailure() {
        var upstream = ApplicationClientProjectionException.retryable(
                ApplicationClientProjectionFailureCode.KEYCLOAK_UNAVAILABLE,
                new IllegalStateException("sensitive upstream detail"));

        var problem = new ClientApplicationAdminExceptionHandler()
                .credentialProviderFailure(upstream);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(problem.getDetail()).doesNotContain("sensitive upstream detail");
    }
}
