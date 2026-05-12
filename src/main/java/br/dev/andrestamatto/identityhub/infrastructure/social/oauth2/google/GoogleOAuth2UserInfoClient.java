package br.dev.andrestamatto.identityhub.infrastructure.social.oauth2.google;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "google-oauth2-userinfo-client",
        url = "${identity-hub.social-login.providers.google.credentials.user-info-url}"
)
public interface GoogleOAuth2UserInfoClient {

    @GetMapping(path = "/v1/userinfo")
    GoogleUserInfoResponse userInfo(@RequestHeader("Authorization") String authorization);
}
