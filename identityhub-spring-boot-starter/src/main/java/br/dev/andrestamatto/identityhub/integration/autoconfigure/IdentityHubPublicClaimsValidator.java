package br.dev.andrestamatto.identityhub.integration.autoconfigure;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

final class IdentityHubPublicClaimsValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_CLAIMS = new OAuth2Error(
            "invalid_token",
            "The access token does not meet the public IdentityHub contract",
            null);

    private final Clock clock;
    private final Duration clockSkew;

    IdentityHubPublicClaimsValidator(Clock clock, Duration clockSkew) {
        this.clock = Objects.requireNonNull(clock);
        this.clockSkew = Objects.requireNonNull(clockSkew);
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (hasRequiredPublicClaims(token) && hasValidAuthorities(token)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(INVALID_CLAIMS);
    }

    private boolean hasRequiredPublicClaims(Jwt token) {
        return nonBlank(token.getSubject())
                && nonBlank(token.getId())
                && isIssuedAtValid(token.getIssuedAt())
                && token.getClaims().containsKey("scope")
                && token.getClaims().containsKey("roles");
    }

    private boolean isIssuedAtValid(Instant issuedAt) {
        return issuedAt != null && !issuedAt.isAfter(Instant.now(clock).plus(clockSkew));
    }

    private boolean hasValidAuthorities(Jwt token) {
        return IdentityHubPublicAuthorityClaims.scopes(token.getClaims().get("scope")).isPresent()
                && IdentityHubPublicAuthorityClaims.roles(token.getClaims().get("roles")).isPresent();
    }

    private boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }
}
