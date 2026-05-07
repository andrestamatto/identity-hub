package br.dev.andrestamatto.identityhub.infrastructure.social.oauth2.google;

import br.dev.andrestamatto.identityhub.application.exception.IdentitySourceUnavailableException;
import br.dev.andrestamatto.identityhub.application.usecase.dto.SocialLoginCommand;
import br.dev.andrestamatto.identityhub.infrastructure.config.IdentityHubSocialLoginProperties;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GoogleOAuth2ProviderClientAdapterTest {

    @Test
    void shouldExchangeCodeAndReturnIdentity() {
        var tokenClient = mock(GoogleOAuth2TokenClient.class);
        var userInfoClient = mock(GoogleOAuth2UserInfoClient.class);
        var provider = new GoogleOAuth2ProviderClientAdapter(tokenClient, userInfoClient, properties());

        when(tokenClient.exchangeCode( any(), any(), any(), any(), any() )).thenReturn(
                new GoogleTokenResponse("token-123", "3600", "refresh-123", "openid email", "Bearer", "id-token")
        );
        when(userInfoClient.userInfo(anyString())).thenReturn(
                new GoogleUserInfoResponse("google-user-1", "user@example.com", true, "User Name", "User", "Name", "http://img")
        );

        var result = provider.fetchIdentity(new SocialLoginCommand("google", "code-123", "http://localhost:8080/oauth2/callback/google"));

        assertEquals("google-user-1", result.providerUserId());
        assertEquals("user@example.com", result.email());
        assertEquals("token-123", result.attributes().get("accessToken"));
    }

    @Test
    void shouldFailWhenAuthorizationCodeIsInvalid() {
        var tokenClient = mock(GoogleOAuth2TokenClient.class);
        var userInfoClient = mock(GoogleOAuth2UserInfoClient.class);
        var provider = new GoogleOAuth2ProviderClientAdapter(tokenClient, userInfoClient, properties());

        when(tokenClient.exchangeCode(any(), any(), any(), any(), any())).thenThrow(feign(400));

        assertThrows(IllegalArgumentException.class,
                () -> provider.fetchIdentity(new SocialLoginCommand("google", "bad-code", "http://localhost:8080/oauth2/callback/google")));
    }

    @Test
    void shouldFailWhenProviderDoesNotReturnEmail() {
        var tokenClient = mock(GoogleOAuth2TokenClient.class);
        var userInfoClient = mock(GoogleOAuth2UserInfoClient.class);
        var provider = new GoogleOAuth2ProviderClientAdapter(tokenClient, userInfoClient, properties());

        when(tokenClient.exchangeCode(any(), any(), any(), any(), any())).thenReturn(
                new GoogleTokenResponse("token-123", "3600", "refresh-123", "openid email", "Bearer", "id-token")
        );
        when(userInfoClient.userInfo(anyString())).thenReturn(
                new GoogleUserInfoResponse("google-user-1", null, true, "User Name", "User", "Name", "http://img")
        );

        assertThrows(IllegalArgumentException.class,
                () -> provider.fetchIdentity(new SocialLoginCommand("google", "code-123", "http://localhost:8080/oauth2/callback/google")));
    }

    @Test
    void shouldFailWhenProviderIsUnavailable() {
        var tokenClient = mock(GoogleOAuth2TokenClient.class);
        var userInfoClient = mock(GoogleOAuth2UserInfoClient.class);
        var provider = new GoogleOAuth2ProviderClientAdapter(tokenClient, userInfoClient, properties());

        when(tokenClient.exchangeCode(any(), any(), any(), any(), any())).thenThrow(feign(503));

        assertThrows(IdentitySourceUnavailableException.class,
                () -> provider.fetchIdentity(new SocialLoginCommand("google", "code-123", "http://localhost:8080/oauth2/callback/google")));
    }

    private IdentityHubSocialLoginProperties properties() {
        var credentials = new IdentityHubSocialLoginProperties.Credentials(
                "id",
                "secret",
                "https://oauth2.googleapis.com",
                "https://openidconnect.googleapis.com",
                Set.of("openid", "profile", "email")
        );
        var provider = new IdentityHubSocialLoginProperties.ProviderProperties(
                true,
                "https://accounts.google.com/o/oauth2/v2/auth",
                credentials,
                "http://localhost:8080/oauth2/callback/google",
                Set.of("http://localhost:8080/oauth2/callback/google")
        );
        return new IdentityHubSocialLoginProperties(true, Map.of("google", provider));
    }

    private FeignException feign(int status) {
        var request = Request.create(Request.HttpMethod.POST, "https://oauth2.googleapis.com/token", Map.of(), null, StandardCharsets.UTF_8, null);
        var response = Response.builder().status(status).request(request).build();
        return FeignException.errorStatus("exchangeCode", response);
    }
}
