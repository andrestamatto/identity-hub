package br.dev.andrestamatto.identityhub.infrastructure.persistence;

import br.dev.andrestamatto.identityhub.infrastructure.persistence.entity.ExternalUserEntity;
import br.dev.andrestamatto.identityhub.infrastructure.persistence.repository.ExternalUserRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocalExternalIdentityAdapterTest {

    @Test
    void shouldMapEntityToUserAndParseCsvSets() throws Exception {
        var repository = mock(ExternalUserRepository.class);
        var adapter = new LocalExternalIdentityAdapter(repository);
        var entity = newEntity();

        setField(entity, "id", UUID.randomUUID());
        setField(entity, "email", "user@identityhub.dev");
        setField(entity, "encodedPassword", "$2a$10$abc");
        setField(entity, "roles", "admin, manager");
        setField(entity, "permissions", "report_read, export_data");

        when(repository.findByEmail("user@identityhub.dev")).thenReturn(Optional.of(entity));

        var userOpt = adapter.load("user@identityhub.dev");
        assertTrue(userOpt.isPresent());
        var user = userOpt.get();

        assertEquals("user@identityhub.dev", user.getIdentity());
        assertEquals("$2a$10$abc", user.getEncodedPassword().value());
        assertEquals(2, user.getRoles().size());
        assertEquals(2, user.getPermissions().size());
        assertTrue(user.getRoles().stream().anyMatch(r -> r.value().equals("ADMIN")));
        assertTrue(user.getPermissions().stream().anyMatch(p -> p.value().equals("REPORT_READ")));
    }

    @Test
    void shouldReturnEmptyWhenRepositoryReturnsEmpty() {
        var repository = mock(ExternalUserRepository.class);
        var adapter = new LocalExternalIdentityAdapter(repository);
        when(repository.findByEmail("missing@identityhub.dev")).thenReturn(Optional.empty());

        assertTrue(adapter.load("missing@identityhub.dev").isEmpty());
    }

    @Test
    void shouldFailWhenCsvContainsInvalidRoleOrPermission() throws Exception {
        var repository = mock(ExternalUserRepository.class);
        var adapter = new LocalExternalIdentityAdapter(repository);
        var entity = newEntity();

        setField(entity, "id", UUID.randomUUID());
        setField(entity, "email", "user@identityhub.dev");
        setField(entity, "encodedPassword", "$2a$10$abc");
        setField(entity, "roles", "USER,invalid-role");
        setField(entity, "permissions", "PROFILE_READ");

        when(repository.findByEmail("user@identityhub.dev")).thenReturn(Optional.of(entity));

        assertThrows(IllegalArgumentException.class, () -> adapter.load("user@identityhub.dev"));
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static ExternalUserEntity newEntity() throws Exception {
        var constructor = ExternalUserEntity.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }
}
