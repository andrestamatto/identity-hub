package br.dev.andrestamatto.identityhub.infrastructure.social.oauth2.google;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleUserInfoResponse(
        @JsonProperty("sub")
        String sub,
        @JsonProperty("email")
        String email,
        @JsonProperty("email_verified")
        Boolean emailVerified,
        @JsonProperty("name")
        String name,
        @JsonProperty("given_name")
        String givenName,
        @JsonProperty("family_name")
        String familyName,
        @JsonProperty("picture")
        String picture,
        @JsonProperty("locale")
        String locale
) {
}
