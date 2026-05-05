package br.dev.andrestamatto.identityhub.infrastructure.social.oauth2.google;

import br.dev.andrestamatto.identityhub.application.exception.IdentitySourceUnavailableException;
import br.dev.andrestamatto.identityhub.application.usecase.dto.SocialLoginCommand;
import br.dev.andrestamatto.identityhub.domain.model.SocialIdentity;
import br.dev.andrestamatto.identityhub.domain.model.SocialProvider;
import br.dev.andrestamatto.identityhub.infrastructure.config.IdentityHubSocialLoginProperties;
import br.dev.andrestamatto.identityhub.infrastructure.social.oauth2.OAuth2ProviderClient;
import feign.FeignException;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;
import java.util.Optional;

@Component
public class GoogleOAuth2ProviderClientAdapter implements OAuth2ProviderClient {

    private static final String AUTHORIZATION_CODE_GRANT_TYPE = "authorization_code";

    private final GoogleOAuth2TokenClient googleOAuth2TokenClient;
    private final GoogleOAuth2UserInfoClient googleOAuth2UserInfoClient;
    private final IdentityHubSocialLoginProperties socialLoginProperties;

    public GoogleOAuth2ProviderClientAdapter(
            GoogleOAuth2TokenClient googleOAuth2TokenClient,
            GoogleOAuth2UserInfoClient googleOAuth2UserInfoClient,
            IdentityHubSocialLoginProperties socialLoginProperties
    ) {
        this.googleOAuth2TokenClient = googleOAuth2TokenClient;
        this.googleOAuth2UserInfoClient = googleOAuth2UserInfoClient;
        this.socialLoginProperties = socialLoginProperties;
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.GOOGLE;
    }

    @Override
    public SocialIdentity fetchIdentity(SocialLoginCommand input) {
        validateInput(input);
        try {
            GoogleTokenResponse tokenResponse = Optional.ofNullable(
                    googleOAuth2TokenClient.exchangeCode(buildTokenRequestForm(input))
            ).orElseThrow(() -> new IllegalArgumentException("Google token response is empty."));

            if (isBlank(tokenResponse.accessToken())) {
                throw new IllegalArgumentException("Google token response does not contain access_token.");
            }

            GoogleUserInfoResponse userInfo = Optional.ofNullable(
                    googleOAuth2UserInfoClient.userInfo("Bearer " + tokenResponse.accessToken())
            ).orElseThrow(() -> new IllegalArgumentException("Google user info response is empty."));

            if (isBlank(userInfo.sub())) {
                throw new IllegalArgumentException("Google user info does not contain user id.");
            }
            if (isBlank(userInfo.email())) {
                throw new IllegalArgumentException("Google user info does not contain email.");
            }

            return new SocialIdentity(provider(), userInfo.sub(), userInfo.email(), buildIdentityAttributes(tokenResponse, userInfo));
        } catch (FeignException.BadRequest exception) {
            throw new IllegalArgumentException("Invalid authorization code for Google provider.", exception);
        } catch (FeignException exception) {
            throw new IdentitySourceUnavailableException("Google provider is unavailable.", exception);
        }
    }

    private void validateInput(SocialLoginCommand input) {
        if (input == null) {
            throw new IllegalArgumentException("Social login input is required.");
        }
        if (isBlank(input.authorizationCode())) {
            throw new IllegalArgumentException("Authorization code is required.");
        }
        if (isBlank(input.redirectUri())) {
            throw new IllegalArgumentException("Redirect URI is required.");
        }
    }

    private MultiValueMap<String, String> buildTokenRequestForm(SocialLoginCommand input) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        var providerProperties = socialLoginProperties.getProviderProperties(provider().getProviderName());
        var redirectUri = Optional.ofNullable(input.redirectUri())
                .filter(value -> !value.isBlank())
                .orElse(providerProperties.defaultRedirectUrl());

        params.add("code", input.authorizationCode());
        params.add("client_id", providerProperties.credentials().clientId());
        params.add("client_secret", providerProperties.credentials().clientSecret());
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", AUTHORIZATION_CODE_GRANT_TYPE);
        return params;
    }

    private Map<String, Object> buildIdentityAttributes(GoogleTokenResponse tokenResponse, GoogleUserInfoResponse userInfo) {
        return Map.of(
                "accessToken", tokenResponse.accessToken(),
                "refreshToken", tokenResponse.refreshToken(),
                "tokenType", tokenResponse.tokenType(),
                "expiresIn", tokenResponse.expiresIn(),
                "scope", tokenResponse.scope(),
                "emailVerified", userInfo.emailVerified(),
                "name", userInfo.name(),
                "givenName", userInfo.givenName(),
                "familyName", userInfo.familyName(),
                "picture", userInfo.picture()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
