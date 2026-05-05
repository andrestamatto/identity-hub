package br.dev.andrestamatto.identityhub.infrastructure.social.oauth2.google;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "google-oauth2-token-client",
        url = "${identity-hub.social-login.providers.google.credentials.token-url}"
)
public interface GoogleOAuth2TokenClient {

    @PostMapping(path = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    GoogleTokenResponse exchangeCode(@RequestBody MultiValueMap<String, String> form);
}
