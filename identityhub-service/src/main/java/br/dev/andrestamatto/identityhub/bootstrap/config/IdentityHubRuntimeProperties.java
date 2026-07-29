package br.dev.andrestamatto.identityhub.bootstrap.config;

import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("identityhub.runtime")
public record IdentityHubRuntimeProperties(DeploymentEnvironment environment) {

    public IdentityHubRuntimeProperties {
        Objects.requireNonNull(environment, "identityhub.runtime.environment is required");
    }

    public enum DeploymentEnvironment {
        DEVELOPMENT,
        PRODUCTION
    }
}
