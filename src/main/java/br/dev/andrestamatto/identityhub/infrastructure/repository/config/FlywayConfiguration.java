package br.dev.andrestamatto.identityhub.infrastructure.repository.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "identity-hub.repository", name = "type", havingValue = "jpa")
public class FlywayConfiguration {}
