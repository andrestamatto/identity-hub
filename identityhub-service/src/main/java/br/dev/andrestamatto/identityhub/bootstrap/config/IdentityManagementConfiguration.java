package br.dev.andrestamatto.identityhub.bootstrap.config;

import br.dev.andrestamatto.identityhub.clientapplication.application.GetClientApplication;
import br.dev.andrestamatto.identityhub.identity.adapter.out.clientapplication.ClientApplicationSelfRegistrationPolicyResolver;
import br.dev.andrestamatto.identityhub.identity.adapter.out.keycloak.KeycloakLocalIdentityRegistrar;
import br.dev.andrestamatto.identityhub.identity.application.RegisterPendingLocalIdentity;
import java.net.http.HttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        name = "identityhub.keycloak.identity-management.enabled",
        havingValue = "true")
class IdentityManagementConfiguration {

    @Bean
    ClientApplicationSelfRegistrationPolicyResolver selfRegistrationPolicyResolver(
            GetClientApplication getClientApplication) {
        return new ClientApplicationSelfRegistrationPolicyResolver(getClientApplication);
    }

    @Bean
    KeycloakLocalIdentityRegistrar localIdentityRegistrar(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            IdentityManagementProperties properties) {
        return new KeycloakLocalIdentityRegistrar(
                httpClient,
                objectMapper,
                properties.baseUri(),
                properties.realm(),
                properties.clientId(),
                properties.clientSecret());
    }

    @Bean
    RegisterPendingLocalIdentity registerPendingLocalIdentity(
            ClientApplicationSelfRegistrationPolicyResolver policyResolver,
            KeycloakLocalIdentityRegistrar registrar) {
        return new RegisterPendingLocalIdentity(policyResolver, registrar);
    }
}
