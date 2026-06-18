package br.dev.andrestamatto.identityhub.infrastructure.support;

import br.dev.andrestamatto.identityhub.application.ports.output.UsernameResolver;
import br.dev.andrestamatto.identityhub.application.ports.output.VerificationTokenGenerator;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;
import java.util.Random;

@Configuration
public class SupportConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public Random random() { return new Random(); }

    @Bean
    public Duration fifteenMinutes() { return Duration.ofMinutes(15); }

    @Bean
    public VerificationTokenGenerator RandomVerificationTokenGenerator(Clock clock,  Random random,  Duration fifteenMinutes) {
        return new RandomVerificationTokenGenerator(clock, random, fifteenMinutes);
    }

    @Bean
    public PhoneNumberUtil phoneNumberUtil() {
        return PhoneNumberUtil.getInstance();
    }

    @Bean
    public UsernameResolver defaultUsernameResolver(PhoneNumberUtil phoneNumberUtil) {
        return new PhoneEmailUsernameResolver(phoneNumberUtil, "BR");
    }

}
