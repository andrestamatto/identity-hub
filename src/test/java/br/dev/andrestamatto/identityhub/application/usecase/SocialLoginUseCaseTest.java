package br.dev.andrestamatto.identityhub.application.usecase;

import br.dev.andrestamatto.identityhub.application.exception.IdentitySourceUnavailableException;
import br.dev.andrestamatto.identityhub.application.ports.LoadSocialIdentityPort;
import br.dev.andrestamatto.identityhub.application.ports.ResolveSocialUserPort;
import br.dev.andrestamatto.identityhub.application.ports.SocialProviderPolicyPort;
import br.dev.andrestamatto.identityhub.application.ports.dto.SocialProviderPolicy;
import br.dev.andrestamatto.identityhub.domain.model.EncodedPassword;
import br.dev.andrestamatto.identityhub.domain.model.PermissionName;
import br.dev.andrestamatto.identityhub.domain.model.RoleName;
import br.dev.andrestamatto.identityhub.domain.model.SocialIdentity;
import br.dev.andrestamatto.identityhub.domain.model.SocialProvider;
import br.dev.andrestamatto.identityhub.domain.model.User;
import br.dev.andrestamatto.identityhub.application.ports.TokenServicePort;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SocialLoginUseCaseTest {

    @Test
    void shouldAuthenticateWhenProviderAndRedirectAreValid() {
        var loadSocialIdentity = mock(LoadSocialIdentityPort.class);
        var resolveSocialUser = mock(ResolveSocialUserPort.class);
        var tokenService = mock(TokenServicePort.class);
        var socialProviderPolicyPort = mock(SocialProviderPolicyPort.class);

        when(socialProviderPolicyPort.enabled()).thenReturn(true);
        when(socialProviderPolicyPort.getProviderPolicy("google")).thenReturn(
                new SocialProviderPolicy(true, "http://localhost:8081/callback", Set.of("http://localhost:8081/callback"))
        );

        var useCase = new SocialLoginUseCase(loadSocialIdentity, resolveSocialUser, tokenService, socialProviderPolicyPort);

        var socialIdentity = new SocialIdentity.Builder()
                .provider(SocialProvider.GOOGLE)
                .providerUserId("google-user-1")
                .email("user@example.com")
                .attributes(Map.of())
                .build();
        var user = new User(
                UUID.randomUUID(),
                "user@example.com",
                EncodedPassword.from("$2a$10$abc"),
                Set.of(RoleName.from("USER")),
                Set.of(PermissionName.from("PROFILE_READ"))
        );

        when(loadSocialIdentity.load(any())).thenReturn(socialIdentity);
        when(resolveSocialUser.resolve(socialIdentity)).thenReturn(user);
        when(tokenService.issue(user)).thenReturn("token-social");
        when(tokenService.accessTokenExpiresInSeconds()).thenReturn(3600L);

        var result = useCase.execute("GOOGLE", "code-123", "http://localhost:8081/callback");

        assertEquals("token-social", result.accessToken());
        assertEquals("Bearer", result.tokenType());
        assertEquals(3600L, result.expiresIn());
    }

    @Test
    void shouldRejectBlankAuthorizationCode() {
        var socialProviderPolicyPort = mock(SocialProviderPolicyPort.class);
        when(socialProviderPolicyPort.enabled()).thenReturn(true);
        when(socialProviderPolicyPort.getProviderPolicy("google")).thenReturn(
                new SocialProviderPolicy(true, "x", Set.of())
        );

        var useCase = new SocialLoginUseCase(
                mock(LoadSocialIdentityPort.class),
                mock(ResolveSocialUserPort.class),
                mock(TokenServicePort.class),
                socialProviderPolicyPort
        );

        assertThrows(IllegalArgumentException.class, () -> useCase.execute("google", "   ", null));
    }

    @Test
    void shouldRejectRedirectUriOutsideAllowedList() {
        var socialProviderPolicyPort = mock(SocialProviderPolicyPort.class);
        when(socialProviderPolicyPort.enabled()).thenReturn(true);
        when(socialProviderPolicyPort.getProviderPolicy("google")).thenReturn(
                new SocialProviderPolicy(true, "http://localhost:8081/callback", Set.of("http://localhost:8081/callback"))
        );

        var useCase = new SocialLoginUseCase(
                mock(LoadSocialIdentityPort.class),
                mock(ResolveSocialUserPort.class),
                mock(TokenServicePort.class),
                socialProviderPolicyPort
        );

        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute("google", "code-123", "http://localhost:9999/other"));
    }

    @Test
    void shouldRejectWhenSocialLoginDisabled() {
        var socialProviderPolicyPort = mock(SocialProviderPolicyPort.class);
        when(socialProviderPolicyPort.enabled()).thenReturn(false);

        var useCase = new SocialLoginUseCase(
                mock(LoadSocialIdentityPort.class),
                mock(ResolveSocialUserPort.class),
                mock(TokenServicePort.class),
                socialProviderPolicyPort
        );

        assertThrows(IdentitySourceUnavailableException.class, () ->
                useCase.execute("google", "code-123", null));
    }
}
