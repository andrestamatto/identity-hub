package br.dev.andrestamatto.identityhub.infrastructure.apis;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "whatsapp-api", url = "${identity-hub.notification.whatsapp.api-url}")
public interface WhatsappApiClient {

    @PostMapping("/send")
    ResponseEntity<WhatsappResponse> send(
            //@RequestHeader("Authorization") String token,
            @RequestBody WhatsappRequest requestContent
    );

    @PostMapping("/send-media")
    ResponseEntity<WhatsappResponse> sendMedia(
            //@RequestHeader("Authorization") String token,
            @RequestBody WhatsappRequest requestContent
    );

}
