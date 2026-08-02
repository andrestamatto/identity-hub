package br.dev.andrestamatto.identityhub.bootstrap.config;

import br.dev.andrestamatto.identityhub.identity.adapter.in.http.InMemoryRegistrationRateLimiter;
import br.dev.andrestamatto.identityhub.identity.adapter.in.http.InMemoryPasswordRecoveryRateLimiter;
import br.dev.andrestamatto.identityhub.identity.adapter.in.http.MinimumPublicResponseTiming;
import br.dev.andrestamatto.identityhub.identity.adapter.in.http.PublicIdentityRequestSizeFilter;
import br.dev.andrestamatto.identityhub.identity.adapter.in.http.PublicResponseTiming;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.locks.LockSupport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "identityhub.public-identity.enabled", havingValue = "true")
class PublicIdentityConfiguration {

    @Bean
    InMemoryRegistrationRateLimiter publicRegistrationRateLimiter(
            PublicIdentityProperties properties,
            Clock clock) {
        return new InMemoryRegistrationRateLimiter(
                properties.registrationRequestLimit(),
                properties.registrationWindow(),
                properties.trackedSourceLimit(),
                clock);
    }

    @Bean
    InMemoryPasswordRecoveryRateLimiter publicPasswordRecoveryRateLimiter(
            PublicIdentityProperties properties,
            Clock clock) {
        return new InMemoryPasswordRecoveryRateLimiter(
                20,
                Duration.ofMinutes(15),
                properties.trackedSourceLimit(),
                clock);
    }

    @Bean
    PublicResponseTiming publicResponseTiming(PublicIdentityProperties properties) {
        return new MinimumPublicResponseTiming(
                properties.minimumResponseTime(),
                System::nanoTime,
                LockSupport::parkNanos);
    }

    @Bean
    PublicIdentityRequestSizeFilter publicIdentityRequestSizeFilter(
            PublicIdentityProperties properties) {
        return new PublicIdentityRequestSizeFilter(properties.maximumRequestBytes());
    }
}
