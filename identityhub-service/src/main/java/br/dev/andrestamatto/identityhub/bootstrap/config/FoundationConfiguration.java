package br.dev.andrestamatto.identityhub.bootstrap.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AlternativeJdkIdGenerator;
import org.springframework.util.IdGenerator;

@Configuration(proxyBeanMethods = false)
class FoundationConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    IdGenerator correlationIdGenerator() {
        return new AlternativeJdkIdGenerator();
    }
}
