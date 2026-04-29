package br.dev.andrestamatto.identityhub.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.mock.env.MockEnvironment;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltInLocalIdentityStoreAutoConfigurationImportFilterTest {

    @Test
    void shouldExcludePersistenceAutoConfigsWhenLocalStoreIsDisabled() {
        var filter = new BuiltInLocalIdentityStoreAutoConfigurationImportFilter();
        var env = new MockEnvironment().withProperty("identity-hub.local-identity-store.enabled", "false");
        filter.setEnvironment(env);

        String[] autoConfigs = {
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
                "org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration"
        };

        boolean[] matches = filter.match(autoConfigs, AutoConfigurationMetadataLoader.empty());

        assertFalse(matches[0]);
        assertFalse(matches[1]);
        assertTrue(matches[2]);
    }

    @Test
    void shouldKeepAutoConfigsWhenLocalStoreIsEnabled() {
        var filter = new BuiltInLocalIdentityStoreAutoConfigurationImportFilter();
        var env = new MockEnvironment().withProperty("identity-hub.local-identity-store.enabled", "true");
        filter.setEnvironment(env);

        String[] autoConfigs = {
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
        };

        boolean[] matches = filter.match(autoConfigs, AutoConfigurationMetadataLoader.empty());

        assertTrue(matches[0]);
        assertTrue(matches[1]);
    }

    private static final class AutoConfigurationMetadataLoader implements AutoConfigurationMetadata {
        static AutoConfigurationMetadata empty() {
            return new AutoConfigurationMetadataLoader();
        }

        @Override
        public boolean wasProcessed(String className) {
            return false;
        }

        @Override
        public Integer getInteger(String className, String key) {
            return null;
        }

        @Override
        public Integer getInteger(String className, String key, Integer defaultValue) {
            return defaultValue;
        }

        @Override
        public Set<String> getSet(String className, String key) {
            return null;
        }

        @Override
        public Set<String> getSet(String className, String key, Set<String> defaultValue) {
            return defaultValue;
        }

        @Override
        public String get(String className, String key) {
            return null;
        }

        @Override
        public String get(String className, String key, String defaultValue) {
            return defaultValue;
        }
    }
}
