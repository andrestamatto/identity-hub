package br.dev.andrestamatto.identityhub.infrastructure.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="identity-hub.media")
public record MediaProperties(
        String baseUrl
) {
}
