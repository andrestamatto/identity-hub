package br.dev.andrestamatto.identityhub.integration.autoconfigure;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Validated runtime configuration for the IdentityHub Resource Server integration.
 */
@ConfigurationProperties("identityhub.security")
public record IdentityHubSecurityProperties(
        @DefaultValue("true") boolean enabled,
        URI issuerUri,
        String audience,
        @DefaultValue("60s") Duration clockSkew,
        boolean allowHttpForLoopback,
        Authorities authorities) {

    private static final Duration DEFAULT_CLOCK_SKEW = Duration.ofSeconds(60);
    private static final Pattern SAFE_AUDIENCE =
            Pattern.compile("^[A-Za-z0-9](?:[A-Za-z0-9._:/-]*[A-Za-z0-9])$");

    public IdentityHubSecurityProperties {
        clockSkew = clockSkew == null ? DEFAULT_CLOCK_SKEW : clockSkew;
        authorities = authorities == null ? Authorities.defaults() : authorities;
        if (enabled) {
            requireIssuer(issuerUri, allowHttpForLoopback);
            requireAudience(audience);
            requireClockSkew(clockSkew);
        }
    }

    private static void requireIssuer(URI issuerUri, boolean allowHttpForLoopback) {
        if (issuerUri == null
                || !issuerUri.isAbsolute()
                || issuerUri.getHost() == null
                || issuerUri.getRawUserInfo() != null
                || issuerUri.getRawQuery() != null
                || issuerUri.getRawFragment() != null) {
            throw new IllegalArgumentException("identityhub.security.issuer-uri must be an absolute issuer URI");
        }
        if ("https".equalsIgnoreCase(issuerUri.getScheme())) {
            return;
        }
        if ("http".equalsIgnoreCase(issuerUri.getScheme())
                && allowHttpForLoopback
                && isLoopback(issuerUri.getHost())) {
            return;
        }
        throw new IllegalArgumentException(
                "identityhub.security.issuer-uri must use HTTPS or explicit loopback HTTP");
    }

    private static boolean isLoopback(String host) {
        var normalized = host.replace("[", "").replace("]", "").toLowerCase(Locale.ROOT);
        return "localhost".equals(normalized)
                || "::1".equals(normalized)
                || normalized.matches("127(?:\\.\\d{1,3}){3}");
    }

    private static void requireAudience(String audience) {
        if (audience == null || audience.length() > 255 || !SAFE_AUDIENCE.matcher(audience).matches()) {
            throw new IllegalArgumentException(
                    "identityhub.security.audience must be an exact safe identifier");
        }
    }

    private static void requireClockSkew(Duration clockSkew) {
        if (clockSkew.isNegative() || clockSkew.compareTo(DEFAULT_CLOCK_SKEW) > 0) {
            throw new IllegalArgumentException("identityhub.security.clock-skew must be between 0s and 60s");
        }
    }

    /**
     * Prefixes applied to public token claims when they become Spring authorities.
     */
    public record Authorities(
            @DefaultValue("ROLE_") String rolePrefix,
            @DefaultValue("SCOPE_") String scopePrefix) {

        private static final String DEFAULT_ROLE_PREFIX = "ROLE_";
        private static final String DEFAULT_SCOPE_PREFIX = "SCOPE_";

        public Authorities {
            rolePrefix = rolePrefix == null ? DEFAULT_ROLE_PREFIX : rolePrefix;
            scopePrefix = scopePrefix == null ? DEFAULT_SCOPE_PREFIX : scopePrefix;
            requirePrefix(rolePrefix, "role-prefix");
            requirePrefix(scopePrefix, "scope-prefix");
        }

        static Authorities defaults() {
            return new Authorities(DEFAULT_ROLE_PREFIX, DEFAULT_SCOPE_PREFIX);
        }

        private static void requirePrefix(String value, String property) {
            if (value.isBlank()) {
                throw new IllegalArgumentException("identityhub.security.authorities." + property + " is required");
            }
        }
    }
}
