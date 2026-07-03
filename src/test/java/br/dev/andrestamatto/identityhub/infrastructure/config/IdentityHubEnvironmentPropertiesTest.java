package br.dev.andrestamatto.identityhub.infrastructure.config;

import br.dev.andrestamatto.identityhub.infrastructure.messaging.config.NotificationProperties;
import br.dev.andrestamatto.identityhub.infrastructure.security.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "IDENTITY_HUB_API_SECRET=test-api-secret",
        "TWILIO_ACCOUNT_SID=test-twilio-account-sid",
        "TWILIO_AUTH_TOKEN=test-twilio-auth-token",
        "TWILIO_SMS_FROM=+15550000001",
        "IDENTITY_HUB_WHATSAPP_API_URL=http://localhost:3000"
})
@ActiveProfiles("test")
public class IdentityHubEnvironmentPropertiesTest {

    @Autowired
    private SecurityProperties securityProperties;

    @Autowired
    private NotificationProperties notificationProperties;

    @Test
    public void shouldReadIdentityHubApiSecretFromEnvironment() {
        assertEquals("test-api-secret", securityProperties.apiSecret());
    }

    @Test
    public void shouldReadTwilioAccountSidFromEnvironment() {
        assertEquals("test-twilio-account-sid", notificationProperties.sms().providers().twilio().accountSid());
    }

    @Test
    public void shouldReadTwilioAuthTokenFromEnvironment() {
        assertEquals("test-twilio-auth-token", notificationProperties.sms().providers().twilio().authToken());
    }

    @Test
    public void shouldReadTwilioSmsFromFromEnvironment() {
        assertEquals("+15550000001", notificationProperties.sms().providers().twilio().from());
    }

    @Test
    public void shouldReadWhatsappApiUrlFromEnvironment() {
        assertEquals("http://localhost:3000", notificationProperties.whatsapp().apiUrl());
    }
}
