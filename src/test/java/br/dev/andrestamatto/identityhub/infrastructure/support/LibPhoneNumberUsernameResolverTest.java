package br.dev.andrestamatto.identityhub.infrastructure.support;

import br.dev.andrestamatto.identityhub.domain.valueobjects.UsernameType;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LibPhoneNumberUsernameResolverTest {

    private LibPhoneNumberUsernameResolver resolver;

    @BeforeEach
    public void setup() {
        resolver = new LibPhoneNumberUsernameResolver(PhoneNumberUtil.getInstance(), "BR");
    }

    @Test
    public void shouldResolveAndNormalizeEmailUsername() {
        var username = resolver.resolve(" User@IdentityHub.com ");

        assertEquals("user@identityhub.com", username.value());
        assertEquals(UsernameType.EMAIL, username.usernameType());
    }

    @Test
    public void shouldResolveAndNormalizeBrazilianPhoneUsername() {
        var username = resolver.resolve("11999998888");

        assertEquals("+5511999998888", username.value());
        assertEquals(UsernameType.PHONE, username.usernameType());
    }

    @Test
    public void shouldResolveInternationalPhoneUsername() {
        var username = resolver.resolve("+14155552671");

        assertEquals("+14155552671", username.value());
        assertEquals(UsernameType.PHONE, username.usernameType());
    }

    @Test
    public void shouldRejectInvalidUsername() {
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("invalidUsername"));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("0123456789"));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(null));
    }
}
