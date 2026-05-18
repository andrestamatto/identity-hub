package br.dev.andrestamatto.identityhub.infrastructure.support;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class SupportConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

}
