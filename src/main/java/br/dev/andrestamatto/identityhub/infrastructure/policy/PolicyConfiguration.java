package br.dev.andrestamatto.identityhub.infrastructure.policy;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({PoliciesProperties.class})
public class PolicyConfiguration {

    @Bean
    public ConfigBasedRegistrationPolicy configBasedRegistrationPolicy(PoliciesProperties policiesProperties) {
        return new ConfigBasedRegistrationPolicy(policiesProperties);
    }
}
