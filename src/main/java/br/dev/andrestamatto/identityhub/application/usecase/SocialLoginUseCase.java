package br.dev.andrestamatto.identityhub.application.usecase;

import br.dev.andrestamatto.identityhub.application.exception.AuthenticationFailedException;
import br.dev.andrestamatto.identityhub.application.exception.IdentitySourceUnavailableException;
import br.dev.andrestamatto.identityhub.application.ports.LoadSocialIdentity;
import br.dev.andrestamatto.identityhub.application.ports.ResolveSocialUser;
import br.dev.andrestamatto.identityhub.application.ports.SocialProviderPolicyPort;
import br.dev.andrestamatto.identityhub.application.ports.dto.SocialProviderPolicy;
import br.dev.andrestamatto.identityhub.application.result.AuthenticationResult;
import br.dev.andrestamatto.identityhub.domain.model.SocialProvider;
import br.dev.andrestamatto.identityhub.application.ports.TokenServicePort;

import java.util.Optional;
import java.util.Set;

public class SocialLoginUseCase implements SocialLogin {

    private static final String TOKEN_TYPE_BEARER = "Bearer";

    private final LoadSocialIdentity loadSocialIdentity;
    private final ResolveSocialUser resolveSocialUser;
    private final TokenServicePort tokenService;
    private final SocialProviderPolicyPort socialProviderPolicyPort;

    public SocialLoginUseCase(
            LoadSocialIdentity loadSocialIdentity,
            ResolveSocialUser resolveSocialUser,
            TokenServicePort tokenService,
            SocialProviderPolicyPort socialProviderPolicyPort
    ) {
        this.loadSocialIdentity = loadSocialIdentity;
        this.resolveSocialUser = resolveSocialUser;
        this.tokenService = tokenService;
        this.socialProviderPolicyPort = socialProviderPolicyPort;
    }

    public AuthenticationResult execute(String provider, String authorizationCode, String redirectUri) {
        validateSocialLoginEnabled();

        SocialProvider socialProvider = SocialProvider.fromString(provider);
        SocialProviderPolicy providerPolicy = socialProviderPolicyPort.getProviderPolicy(socialProvider.getProviderName());

        validateAuthorizationCode(authorizationCode);
        validateProviderEnabled(socialProvider, providerPolicy);

        String effectiveRedirectUri = resolveRedirectUri(providerPolicy, redirectUri);
        var socialLoginInput = new SocialLoginInput(
                socialProvider.getProviderName(),
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
        if (!socialProviderPolicyPort.enabled()) {
            throw new IdentitySourceUnavailableException("Social login is disabled in configuration.");
        }
    }

    private void validateProviderEnabled(SocialProvider provider, SocialProviderPolicy providerPolicy) {
        if (!providerPolicy.enabled()) {
            throw new IllegalArgumentException("Provider is disabled: " + provider.getProviderName());
        }
    }

    private String resolveRedirectUri(SocialProviderPolicy providerPolicy, String requestRedirectUri) {
        String normalizedRequestRedirectUri = normalize(requestRedirectUri);
        if (!normalizedRequestRedirectUri.isBlank()) {
            validateRedirectUriAllowed(providerPolicy, normalizedRequestRedirectUri);
            return normalizedRequestRedirectUri;
        }

        String defaultRedirectUri = normalize(providerPolicy.defaultRedirectUrl());
        if (defaultRedirectUri.isBlank()) {
            throw new IllegalArgumentException("Redirect URI is required for social login.");
        }
        return defaultRedirectUri;
    }

    private void validateRedirectUriAllowed(SocialProviderPolicy providerPolicy, String redirectUri) {
        Set<String> allowed = providerPolicy.allowedRedirectUrls();
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
