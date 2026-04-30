package br.dev.andrestamatto.identityhub.domain.service;

import br.dev.andrestamatto.identityhub.application.ports.LoadExternalIdentity;
import br.dev.andrestamatto.identityhub.domain.model.EncodedPassword;
import br.dev.andrestamatto.identityhub.domain.model.PermissionName;
import br.dev.andrestamatto.identityhub.domain.model.RawPassword;
import br.dev.andrestamatto.identityhub.domain.model.RoleName;
import br.dev.andrestamatto.identityhub.domain.model.User;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginProviderTest {

    @Test
    void shouldReturnUserWhenIdentityExistsAndPasswordMatches() {
        var encoder = mock(PasswordEncoder.class);
        var externalIdentity = mock(LoadExternalIdentity.class);
        var provider = new PasswordLoginAuthenticatorService(encoder, externalIdentity);

        var raw = RawPassword.from("Password@123");
        var user = new User(
                UUID.randomUUID(),
                "user@dev.local",
                EncodedPassword.from("$2a$10$abc"),
                Set.of(RoleName.from("USER")),
                Set.of(PermissionName.from("PROFILE_READ"))
        );

        when(externalIdentity.load("user@dev.local")).thenReturn(Optional.of(user));
        when(encoder.matches(raw, user.getEncodedPassword())).thenReturn(true);

        var authenticated = provider.authenticate("user@dev.local", raw);
        assertNotNull(authenticated);
        assertEquals(user.getId(), authenticated.getId());
    }

    @Test
    void shouldReturnNullWhenPasswordDoesNotMatch() {
        var encoder = mock(PasswordEncoder.class);
        var externalIdentity = mock(LoadExternalIdentity.class);
        var provider = new PasswordLoginAuthenticatorService(encoder, externalIdentity);

        var raw = RawPassword.from("WrongPassword");
        var user = new User(
                UUID.randomUUID(),
                "user@dev.local",
                EncodedPassword.from("$2a$10$abc"),
                Set.of(RoleName.from("USER")),
                Set.of()
        );

        when(externalIdentity.load("user@dev.local")).thenReturn(Optional.of(user));
        when(encoder.matches(raw, user.getEncodedPassword())).thenReturn(false);

        assertNull(provider.authenticate("user@dev.local", raw));
    }

    @Test
    void shouldReturnNullWhenUserDoesNotExist() {
        var encoder = mock(PasswordEncoder.class);
        var externalIdentity = mock(LoadExternalIdentity.class);
        var provider = new PasswordLoginAuthenticatorService(encoder, externalIdentity);

        when(externalIdentity.load("missing@dev.local")).thenReturn(Optional.empty());

        assertNull(provider.authenticate("missing@dev.local", RawPassword.from("Password@123")));
        verifyNoInteractions(encoder);
    }
}
