package br.dev.andrestamatto.identityhub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "IDENTITY_HUB_API_SECRET=test-api-secret"
})
@ActiveProfiles("test")
class IdentityHubApplicationTests {

    @Test
    void contextLoads() {
    }
}
