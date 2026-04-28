package br.dev.andrestamatto.identityhub.infrastructure.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class FakePersistenceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String FAKE_PERSISTENCE_ENABLED = "identity-hub.fake-persistence.enabled";
    private static final String AUTOCONFIGURE_EXCLUDE = "spring.autoconfigure.exclude";
    private static final String SOURCE_NAME = "identityHubFakePersistenceAutoConfig";

    private static final String[] PERSISTENCE_AUTOCONFIG_EXCLUDES = {
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
            "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
            "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
            "org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration",
            "org.springframework.boot.autoconfigure.h2.H2ConsoleAutoConfiguration"
    };

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean fakePersistenceEnabled = environment.getProperty(FAKE_PERSISTENCE_ENABLED, Boolean.class, true);
        if (fakePersistenceEnabled) {
            return;
        }

        String existingExcludes = environment.getProperty(AUTOCONFIGURE_EXCLUDE, "");
        Set<String> mergedExcludes = new LinkedHashSet<>();
        if (!existingExcludes.isBlank()) {
            mergedExcludes.addAll(Arrays.asList(existingExcludes.split(",")));
        }
        mergedExcludes.addAll(Arrays.asList(PERSISTENCE_AUTOCONFIG_EXCLUDES));

        String merged = mergedExcludes.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .reduce((left, right) -> left + "," + right)
                .orElse("");

        environment.getPropertySources().addFirst(new MapPropertySource(
                SOURCE_NAME,
                Map.of(AUTOCONFIGURE_EXCLUDE, merged)
        ));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
