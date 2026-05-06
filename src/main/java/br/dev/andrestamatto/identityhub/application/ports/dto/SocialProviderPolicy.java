package br.dev.andrestamatto.identityhub.application.ports.dto;

import br.dev.andrestamatto.identityhub.domain.model.SocialProvider;

import java.util.Set;

public record SocialProviderPolicy(
        boolean enabled,
        SocialProvider socialProvider,
        String baseUri,
        String defaultRedirectUrl,
        Set<String> allowedRedirectUrls,
        Credentials credentials
) {

    public record Credentials(
           String clientId,
           String clientSecret,
           String tokenUrl,
           String userInfoUrl,
           Set<String> scopes
    ){}

}
