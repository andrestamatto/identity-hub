package br.dev.andrestamatto.identityhub.application.usecase;

import br.dev.andrestamatto.identityhub.application.exception.IdentitySourceUnavailableException;
import br.dev.andrestamatto.identityhub.application.ports.LoadSocialIdentity;
import br.dev.andrestamatto.identityhub.application.ports.ResolveSocialUser;
import br.dev.andrestamatto.identityhub.domain.model.EncodedPassword;
import br.dev.andrestamatto.identityhub.domain.model.PermissionName;
import br.dev.andrestamatto.identityhub.domain.model.RoleName;
import br.dev.andrestamatto.identityhub.domain.model.SocialIdentity;
import br.dev.andrestamatto.identityhub.domain.model.SocialProvider;
import br.dev.andrestamatto.identityhub.domain.model.User;
import br.dev.andrestamatto.identityhub.infrastructure.config.IdentityHubSocialLoginProperties;
import br.dev.andrestamatto.identityhub.infrastructure.security.TokenService;
import org.junit.jupiter.api.Test;

import java.util.List;
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
        var loadSocialIdentity = mock(LoadSocialIdentity.class);
        var resolveSocialUser = mock(ResolveSocialUser.class);
        var tokenService = mock(TokenService.class);

        var providerProperties = new IdentityHubSocialLoginProperties.ProviderProperties(
                true,
                "http://localhost:8081/callback",
                List.of("http://localhost:8081/callback")
        );
        var properties = new IdentityHubSocialLoginProperties(true, Map.of("google", providerProperties));
        var useCase = new SocialLoginUseCase(loadSocialIdentity, resolveSocialUser, tokenService, properties);

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
        var useCase = new SocialLoginUseCase(
                mock(LoadSocialIdentity.class),
                mock(ResolveSocialUser.class),
                mock(TokenService.class),
                new IdentityHubSocialLoginProperties(
                        true,
                        Map.of("google", new IdentityHubSocialLoginProperties.ProviderProperties(true, "x", List.of()))
                )
        );

        assertThrows(IllegalArgumentException.class, () -> useCase.execute("google", "   ", null));
    }

    @Test
    void shouldRejectRedirectUriOutsideAllowedList() {
        var useCase = new SocialLoginUseCase(
                mock(LoadSocialIdentity.class),
                mock(ResolveSocialUser.class),
                mock(TokenService.class),
                new IdentityHubSocialLoginProperties(
                        true,
                        Map.of("google", new IdentityHubSocialLoginProperties.ProviderProperties(
                                true,
                                "http://localhost:8081/callback",
                                List.of("http://localhost:8081/callback")
                        ))
                )
        );

        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute("google", "code-123", "http://localhost:9999/other"));
    }

    @Test
    void shouldRejectWhenSocialLoginDisabled() {
        var useCase = new SocialLoginUseCase(
                mock(LoadSocialIdentity.class),
                mock(ResolveSocialUser.class),
                mock(TokenService.class),
                new IdentityHubSocialLoginProperties(false, Map.of())
        );

        assertThrows(IdentitySourceUnavailableException.class, () ->
                useCase.execute("google", "code-123", null));
    }
}
