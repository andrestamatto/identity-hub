package br.dev.andrestamatto.identityhub.application.usecase;

import br.dev.andrestamatto.identityhub.application.exception.AuthenticationFailedException;
import br.dev.andrestamatto.identityhub.application.exception.IdentitySourceUnavailableException;
import br.dev.andrestamatto.identityhub.application.ports.LoadSocialIdentity;
import br.dev.andrestamatto.identityhub.application.ports.ResolveSocialUser;
import br.dev.andrestamatto.identityhub.application.result.AuthenticationResult;
import br.dev.andrestamatto.identityhub.domain.model.SocialProvider;
import br.dev.andrestamatto.identityhub.infrastructure.config.IdentityHubSocialLoginProperties;
import br.dev.andrestamatto.identityhub.infrastructure.security.TokenService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SocialLoginUseCase implements SocialLogin {

    private static final String TOKEN_TYPE_BEARER = "Bearer";

    private final LoadSocialIdentity loadSocialIdentity;
    private final ResolveSocialUser resolveSocialUser;
    private final TokenService tokenService;
    private final IdentityHubSocialLoginProperties socialLoginProperties;

    public SocialLoginUseCase(
            LoadSocialIdentity loadSocialIdentity,
            ResolveSocialUser resolveSocialUser,
            TokenService tokenService,
            IdentityHubSocialLoginProperties socialLoginProperties
    ) {
        this.loadSocialIdentity = loadSocialIdentity;
        this.resolveSocialUser = resolveSocialUser;
        this.tokenService = tokenService;
        this.socialLoginProperties = socialLoginProperties;
    }

    public AuthenticationResult execute(String provider, String authorizationCode, String redirectUri) {
        validateSocialLoginEnabled();
        validateAuthorizationCode(authorizationCode);

        SocialProvider socialProvider = SocialProvider.fromString(provider);
        var providerProperties = getProviderProperties(socialProvider);
        validateProviderEnabled(socialProvider, providerProperties);

        String effectiveRedirectUri = resolveRedirectUri(providerProperties, redirectUri);
        var socialLoginInput = new SocialLoginInput(
                socialProvider.getProvider(),
                authorizationCode,
                effectiveRedirectUri
        );

        return Optional.ofNullable(loadSocialIdentity.load(socialLoginInput))
                .map(resolveSocialUser::resolve)
                .map(user -> {
                    var token = tokenService.issue(user);
                    return new AuthenticationResult(
                            token,
                            TOKEN_TYPE_BEARER,
                            tokenService.accessTokenExpiresInSeconds()
                    );
                })
                .orElseThrow(AuthenticationFailedException::new);
    }

    private void validateSocialLoginEnabled() {
        if (!socialLoginProperties.enabled()) {
            throw new IdentitySourceUnavailableException("Social login is disabled in configuration.");
        }
    }

    private IdentityHubSocialLoginProperties.ProviderProperties getProviderProperties(SocialProvider provider) {
        Map<String, IdentityHubSocialLoginProperties.ProviderProperties> providers = socialLoginProperties.providers();
        if (providers == null) {
            throw new IllegalArgumentException("No social providers configured.");
        }
        var properties = providers.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getKey().equalsIgnoreCase(provider.getProvider()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
        if (properties == null) {
            throw new IllegalArgumentException("Provider is not configured: " + provider.getProvider());
        }
        return properties;
    }

    private void validateProviderEnabled(SocialProvider provider, IdentityHubSocialLoginProperties.ProviderProperties providerProperties) {
        if (!providerProperties.enabled()) {
            throw new IllegalArgumentException("Provider is disabled: " + provider.getProvider());
        }
    }

    private String resolveRedirectUri(IdentityHubSocialLoginProperties.ProviderProperties providerProperties, String requestRedirectUri) {
        String normalizedRequestRedirectUri = normalize(requestRedirectUri);
        if (!normalizedRequestRedirectUri.isBlank()) {
            validateRedirectUriAllowed(providerProperties, normalizedRequestRedirectUri);
            return normalizedRequestRedirectUri;
        }

        String defaultRedirectUri = normalize(providerProperties.defaultRedirectUrl());
        if (defaultRedirectUri.isBlank()) {
            throw new IllegalArgumentException("Redirect URI is required for social login.");
        }
        return defaultRedirectUri;
    }

    private void validateRedirectUriAllowed(IdentityHubSocialLoginProperties.ProviderProperties providerProperties, String redirectUri) {
        List<String> allowed = providerProperties.allowedRedirectUrls();
        if (allowed == null || allowed.isEmpty()) {
            return;
        }
        boolean allowedRedirectUri = allowed.stream()
                .map(this::normalize)
                .anyMatch(redirectUri::equals);
        if (!allowedRedirectUri) {
            throw new IllegalArgumentException("Redirect URI is not allowed: " + redirectUri);
        }
    }

    private void validateAuthorizationCode(String authorizationCode) {
        if (normalize(authorizationCode).isBlank()) {
            throw new IllegalArgumentException("Authorization code is required.");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
