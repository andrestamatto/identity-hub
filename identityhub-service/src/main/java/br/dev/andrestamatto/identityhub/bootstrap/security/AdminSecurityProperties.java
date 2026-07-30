package br.dev.andrestamatto.identityhub.bootstrap.security;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("identityhub.security.admin")
public record AdminSecurityProperties(URI issuerUri, URI jwkSetUri, String audience) {

    public AdminSecurityProperties {
        requireAbsoluteHttpUri(issuerUri, "issuer-uri");
        requireAbsoluteHttpUri(jwkSetUri, "jwk-set-uri");
        if (audience == null || audience.isBlank()) {
            throw new IllegalArgumentException("identityhub.security.admin.audience is required");
        }
    }

    private static void requireAbsoluteHttpUri(URI uri, String property) {
        Objects.requireNonNull(uri, "identityhub.security.admin." + property + " is required");
        if (!uri.isAbsolute() || !List.of("http", "https").contains(uri.getScheme())) {
            throw new IllegalArgumentException(
                    "identityhub.security.admin." + property + " must be an absolute HTTP URI");
        }
    }
}
