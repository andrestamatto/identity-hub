package br.dev.andrestamatto.identityhub.clientapplication.domain;

import java.util.List;
import java.util.Objects;

public record SpaSettings(
        List<SpaRedirectUri> redirectUris,
        List<WebOrigin> webOrigins) implements ApplicationClientSettings {

    private static final int MAX_ENDPOINTS = 10;

    public SpaSettings {
        redirectUris = List.copyOf(Objects.requireNonNull(redirectUris));
        webOrigins = List.copyOf(Objects.requireNonNull(webOrigins));
        if (redirectUris.isEmpty() || redirectUris.size() > MAX_ENDPOINTS
                || webOrigins.isEmpty() || webOrigins.size() > MAX_ENDPOINTS
                || redirectUris.stream().distinct().count() != redirectUris.size()
                || webOrigins.stream().distinct().count() != webOrigins.size()) {
            throw new IllegalArgumentException("SPA endpoints must be unique and contain 1 to 10 values");
        }
        var allowedOrigins = webOrigins.stream().map(WebOrigin::value).toList();
        if (redirectUris.stream().map(SpaRedirectUri::origin)
                .anyMatch(origin -> !allowedOrigins.contains(origin))) {
            throw new IllegalArgumentException("Every SPA redirect URI must use an allowed web origin");
        }
    }

    public static SpaSettings create(
            List<String> redirectUris,
            List<String> webOrigins,
            SpaTransportPolicy transportPolicy) {
        Objects.requireNonNull(transportPolicy);
        var redirects = Objects.requireNonNull(redirectUris).stream()
                .map(SpaRedirectUri::new)
                .toList();
        var origins = Objects.requireNonNull(webOrigins).stream()
                .map(WebOrigin::new)
                .toList();
        redirects.forEach(transportPolicy::validate);
        origins.forEach(transportPolicy::validate);
        return new SpaSettings(redirects, origins);
    }

    @Override
    public ApplicationClientType type() {
        return ApplicationClientType.SPA;
    }
}
