package br.dev.andrestamatto.identityhub.bootstrap.config;

import java.time.Clock;
import java.time.Duration;
import java.net.http.HttpClient;

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
    HttpClient healthCheckHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @Bean
    IdGenerator correlationIdGenerator() {
        return new AlternativeJdkIdGenerator();
    }
}
