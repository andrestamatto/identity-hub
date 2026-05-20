package br.dev.andrestamatto.identityhub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
        "identity-hub.repository.type=in-memory"
})
class IdentityHubApplicationTests {

    @Test
    void contextLoads() {
    }
}
