package br.dev.andrestamatto.identityhub.clientapplication.domain;

public enum SpaTransportPolicy {
    DEVELOPMENT,
    PRODUCTION;

    void validate(SpaRedirectUri redirectUri) {
        if (!redirectUri.usesHttps()
                && (this == PRODUCTION || !redirectUri.usesLoopbackHost())) {
            throw new IllegalArgumentException("SPA redirect URI must use an allowed secure transport");
        }
    }

    void validate(WebOrigin origin) {
        if (!origin.usesHttps()
                && (this == PRODUCTION || !origin.usesLoopbackHost())) {
            throw new IllegalArgumentException("Web origin must use an allowed secure transport");
        }
    }
}
