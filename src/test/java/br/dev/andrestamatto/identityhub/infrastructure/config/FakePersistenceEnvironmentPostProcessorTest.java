package br.dev.andrestamatto.identityhub.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class FakePersistenceEnvironmentPostProcessorTest {

    @Test
    void shouldAddPersistenceAutoConfigExcludesWhenFakePersistenceIsDisabled() {
        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.getSystemProperties().put("identity-hub.fake-persistence.enabled", "false");

        var processor = new FakePersistenceEnvironmentPostProcessor();
        processor.postProcessEnvironment(environment, new SpringApplication());

        String excludes = environment.getProperty("spring.autoconfigure.exclude");
        assertNotNull(excludes);
        assertTrue(excludes.contains("DataSourceAutoConfiguration"));
        assertTrue(excludes.contains("HibernateJpaAutoConfiguration"));
        assertTrue(excludes.contains("JpaRepositoriesAutoConfiguration"));
    }

    @Test
    void shouldNotAddExcludesWhenFakePersistenceIsEnabled() {
        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.getSystemProperties().put("identity-hub.fake-persistence.enabled", "true");

        var processor = new FakePersistenceEnvironmentPostProcessor();
        processor.postProcessEnvironment(environment, new SpringApplication());

        assertNull(environment.getProperty("spring.autoconfigure.exclude"));
    }
}
