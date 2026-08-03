package br.dev.andrestamatto.identityhub.bootstrap.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

final class IntegrationAudienceValidator implements OAuth2TokenValidator<Jwt> {

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (token.getAudience().contains(
                IntegrationSecurityConfiguration.INTEGRATION_AUDIENCE)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                "invalid_token",
                "Token does not contain the integration audience",
                null));
    }
}
