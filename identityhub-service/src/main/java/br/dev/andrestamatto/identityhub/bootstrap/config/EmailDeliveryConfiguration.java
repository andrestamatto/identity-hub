package br.dev.andrestamatto.identityhub.bootstrap.config;

import br.dev.andrestamatto.identityhub.clientapplication.application.GetClientApplication;
import br.dev.andrestamatto.identityhub.communication.adapter.out.clientapplication.ClientApplicationEmailOriginResolver;
import br.dev.andrestamatto.identityhub.communication.adapter.out.jdbc.JdbcEmailDeliveryRepository;
import br.dev.andrestamatto.identityhub.communication.adapter.out.smtp.SmtpEmailDeliverySender;
import br.dev.andrestamatto.identityhub.communication.application.EmailDeliveryRepository;
import br.dev.andrestamatto.identityhub.communication.application.GetEmailDelivery;
import br.dev.andrestamatto.identityhub.communication.application.PasswordChangedEmailRenderer;
import br.dev.andrestamatto.identityhub.communication.application.ProcessEmailDelivery;
import br.dev.andrestamatto.identityhub.communication.application.RequeueEmailDelivery;
import br.dev.andrestamatto.identityhub.communication.application.RequestPasswordChangedEmail;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.support.TransactionOperations;

@Configuration(proxyBeanMethods = false)
class EmailDeliveryConfiguration {

    @Bean
    EmailDeliveryRepository emailDeliveryRepository(
            JdbcClient jdbcClient,
            TransactionOperations transactions) {
        return new JdbcEmailDeliveryRepository(jdbcClient, transactions);
    }

    @Bean
    ClientApplicationEmailOriginResolver emailOriginResolver(
            GetClientApplication getClientApplication,
            IdentityHubRuntimeProperties runtimeProperties) {
        return new ClientApplicationEmailOriginResolver(
                getClientApplication,
                runtimeProperties.environment().name().toLowerCase());
    }

    @Bean
    RequestPasswordChangedEmail requestPasswordChangedEmail(
            EmailDeliveryRepository repository,
            ClientApplicationEmailOriginResolver originResolver,
            Clock clock) {
        return new RequestPasswordChangedEmail(repository, originResolver, clock);
    }

    @Bean
    GetEmailDelivery getEmailDelivery(EmailDeliveryRepository repository) {
        return new GetEmailDelivery(repository);
    }

    @Bean
    RequeueEmailDelivery requeueEmailDelivery(
            EmailDeliveryRepository repository,
            Clock clock) {
        return new RequeueEmailDelivery(repository, clock);
    }

    @Bean
    @ConditionalOnProperty(
            name = "identityhub.communication.email.enabled",
            havingValue = "true")
    SmtpEmailDeliverySender emailDeliverySender(
            JavaMailSender mailSender,
            EmailDeliveryProperties properties) {
        return new SmtpEmailDeliverySender(mailSender, properties.fromAddress());
    }

    @Bean
    @ConditionalOnProperty(
            name = "identityhub.communication.email.enabled",
            havingValue = "true")
    ProcessEmailDelivery processEmailDelivery(
            EmailDeliveryRepository repository,
            SmtpEmailDeliverySender sender,
            Clock clock,
            EmailDeliveryProperties properties) {
        return new ProcessEmailDelivery(
                repository,
                sender,
                new PasswordChangedEmailRenderer(),
                clock,
                properties.leaseDuration(),
                properties.initialRetryDelay(),
                properties.maxAttempts());
    }

    @Bean
    @ConditionalOnProperty(
            name = "identityhub.communication.email.enabled",
            havingValue = "true")
    EmailDeliveryScheduler emailDeliveryScheduler(
            ProcessEmailDelivery processor,
            MeterRegistry registry) {
        return new EmailDeliveryScheduler(processor, registry, UUID.randomUUID());
    }
}
