package br.dev.andrestamatto.identityhub.infrastructure.policy;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="identity-hub.policies")
public record PoliciesProperties(
    UsernameTypes usernameTypePolicies
) {
    @ConfigurationProperties(prefix="identity-hub.policies.username-types")
    public record UsernameTypes(boolean enableVerificationUponRegistration) {
    }
}