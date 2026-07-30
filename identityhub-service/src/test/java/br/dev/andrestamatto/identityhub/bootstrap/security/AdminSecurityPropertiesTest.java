package br.dev.andrestamatto.identityhub.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class AdminSecurityPropertiesTest {

    @Test
    void bindsTheExactAdministrativeTokenContract() {
        var source = new MapConfigurationPropertySource(Map.of(
                "identityhub.security.admin.issuer-uri", "https://auth.dev.example/realms/identityhub",
                "identityhub.security.admin.jwk-set-uri",
                        "https://auth.dev.example/realms/identityhub/protocol/openid-connect/certs",
                "identityhub.security.admin.audience", "identityhub-admin-api"));

        var properties = new Binder(source)
                .bind("identityhub.security.admin", Bindable.of(AdminSecurityProperties.class))
                .orElseThrow(IllegalStateException::new);

        assertThat(properties.issuerUri().toString())
                .isEqualTo("https://auth.dev.example/realms/identityhub");
        assertThat(properties.audience()).isEqualTo("identityhub-admin-api");
    }
}
