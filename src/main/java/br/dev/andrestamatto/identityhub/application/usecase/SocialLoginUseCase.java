package br.dev.andrestamatto.identityhub.application.usecase;

import br.dev.andrestamatto.identityhub.application.exception.AuthenticationFailedException;
import br.dev.andrestamatto.identityhub.application.exception.IdentitySourceUnavailableException;
import br.dev.andrestamatto.identityhub.application.ports.LoadSocialIdentityPort;
import br.dev.andrestamatto.identityhub.application.ports.ResolveSocialUserPort;
import br.dev.andrestamatto.identityhub.application.ports.SocialProviderPolicyPort;
import br.dev.andrestamatto.identityhub.application.ports.TokenServicePort;
import br.dev.andrestamatto.identityhub.application.ports.dto.SocialProviderPolicy;
import br.dev.andrestamatto.identityhub.application.result.AuthenticationResult;
import br.dev.andrestamatto.identityhub.application.result.AuthorizationResult;
import br.dev.andrestamatto.identityhub.application.usecase.dto.SocialLoginCommand;
import br.dev.andrestamatto.identityhub.application.usecase.port.in.SocialLoginUseCasePort;
import br.dev.andrestamatto.identityhub.domain.model.SocialProvider;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class SocialLoginUseCase implements SocialLoginUseCasePort {

    private static final String TOKEN_TYPE_BEARER = "Bearer";

    private final LoadSocialIdentityPort loadSocialIdentityPort;
    private final ResolveSocialUserPort resolveSocialUserPort;
    private final TokenServicePort tokenService;
    private final SocialProviderPolicyPort socialProviderPolicyPort;

    public SocialLoginUseCase(
            LoadSocialIdentityPort loadSocialIdentityPort,
            ResolveSocialUserPort resolveSocialUserPort,
            TokenServicePort tokenService,
            SocialProviderPolicyPort socialProviderPolicyPort
    ) {
        this.loadSocialIdentityPort = loadSocialIdentityPort;
        this.resolveSocialUserPort = resolveSocialUserPort;
        this.tokenService = tokenService;
        this.socialProviderPolicyPort = socialProviderPolicyPort;
    }


    @Override
    public AuthorizationResult requestAuthorization(String provider) {
        var validatedSocialProviderPolicy = retrieveValidatedSocialProviderPolicy(provider);

        String effectiveRedirectUri = resolveRedirectUri(validatedSocialProviderPolicy, null);

        String state = UUID.randomUUID().toString(); /* TODO: store on session (assignment + expiration) */
        String scope = String.join(" ", validatedSocialProviderPolicy.credentials().scopes());

        String authUrl = UriComponentsBuilder
                .fromUriString(validatedSocialProviderPolicy.baseUri())
                .queryParam("response_type", "code")
                .queryParam("client_id", validatedSocialProviderPolicy.credentials().clientId())
                .queryParam("redirect_uri", effectiveRedirectUri)
                .queryParam("scope", scope)
                .queryParam("state", state)
                .encode()
                .build()
                .toUriString();

        return new AuthorizationResult(URI.create(authUrl), state);
    }

    @Override
    public AuthenticationResult execute(String provider, String authorizationCode, String redirectUri) {

        var validatedSocialProviderPolicy = retrieveValidatedSocialProviderPolicy(provider);
        validateAuthorizationCode(authorizationCode);

        String effectiveRedirectUri = resolveRedirectUri(validatedSocialProviderPolicy, redirectUri);
        var socialLoginInput = new SocialLoginCommand(
                validatedSocialProviderPolicy.socialProvider().getProviderName(),
                authorizationCode,
                effectiveRedirectUri
        );

        return Optional.ofNullable(loadSocialIdentityPort.load(socialLoginInput))
                .map(resolveSocialUserPort::resolve)
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

    private SocialProviderPolicy retrieveValidatedSocialProviderPolicy(String socialProviderStr) {
        validateSocialLoginEnabled();

        var socialProvider = SocialProvider.fromString(socialProviderStr);
        var socialProviderPolicy = socialProviderPolicyPort.getSocialProviderPolicy(socialProvider.getProviderName());

        validateProviderEnabled(socialProviderPolicy);

        return socialProviderPolicy;
    }

    private void validateSocialLoginEnabled() {
        if (!socialProviderPolicyPort.enabled()) {
            throw new IdentitySourceUnavailableException("Social login is disabled in configuration.");
        }
    }

    private void validateProviderEnabled(SocialProviderPolicy providerPolicy) {
        if (!providerPolicy.enabled()) {
            throw new IllegalArgumentException("Provider is disabled: " + providerPolicy.socialProvider().getProviderName());
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
