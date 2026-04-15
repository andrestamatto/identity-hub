package br.dev.andrestamatto.identityhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class IdentityHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityHubApplication.class, args);
    }
}
