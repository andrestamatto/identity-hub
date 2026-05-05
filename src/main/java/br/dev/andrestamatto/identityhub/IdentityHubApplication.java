package br.dev.andrestamatto.identityhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class IdentityHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityHubApplication.class, args);
    }
}
