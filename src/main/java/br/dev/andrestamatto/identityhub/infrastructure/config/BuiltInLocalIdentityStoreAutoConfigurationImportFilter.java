package br.dev.andrestamatto.identityhub.infrastructure.config;

import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.util.Set;

public class BuiltInLocalIdentityStoreAutoConfigurationImportFilter implements AutoConfigurationImportFilter, EnvironmentAware {

    private static final String LOCAL_IDENTITY_STORE_ENABLED = "identity-hub.local-identity-store.enabled";

    private static final Set<String> LOCAL_STORE_AUTOCONFIG_CLASSES = Set.of(
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
            "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
            "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
            "org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration",
            "org.springframework.boot.autoconfigure.h2.H2ConsoleAutoConfiguration"
    );

    private Environment environment;

    @Override
    public boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata autoConfigurationMetadata) {
        boolean localStoreEnabled = environment == null
                || environment.getProperty(LOCAL_IDENTITY_STORE_ENABLED, Boolean.class, true);
        boolean[] matches = new boolean[autoConfigurationClasses.length];

        for (int i = 0; i < autoConfigurationClasses.length; i++) {
            String autoConfigClass = autoConfigurationClasses[i];
            matches[i] = localStoreEnabled
                    || autoConfigClass == null
                    || !LOCAL_STORE_AUTOCONFIG_CLASSES.contains(autoConfigClass);
        }

        return matches;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
