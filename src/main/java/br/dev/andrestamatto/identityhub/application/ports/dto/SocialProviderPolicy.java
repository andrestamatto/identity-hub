package br.dev.andrestamatto.identityhub.application.ports.dto;

import java.util.Set;

public record SocialProviderPolicy(
        boolean enabled,
        String defaultRedirectUrl,
        Set<String> allowedRedirectUrls
) {
}
