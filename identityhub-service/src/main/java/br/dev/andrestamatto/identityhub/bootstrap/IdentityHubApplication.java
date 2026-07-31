package br.dev.andrestamatto.identityhub.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(
        scanBasePackages = "br.dev.andrestamatto.identityhub",
        exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class IdentityHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityHubApplication.class, args);
    }
}
