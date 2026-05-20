package br.dev.andrestamatto.identityhub.infrastructure.policy;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="identity-hub.policies.user-registration")
public record PoliciesProperties(
    UsernameTypePolicy usernameTypePolicy
) {
    public record UsernameTypePolicy(boolean enableVerificationUponRegistration) {
    }
}