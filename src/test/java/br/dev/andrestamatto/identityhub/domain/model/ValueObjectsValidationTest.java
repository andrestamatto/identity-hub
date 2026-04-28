package br.dev.andrestamatto.identityhub.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValueObjectsValidationTest {

    @Test
    void roleNameShouldNormalizeAndValidate() {
        var role = RoleName.from(" admin ");
        assertEquals("ADMIN", role.value());

        assertThrows(IllegalArgumentException.class, () -> RoleName.from("admin-role"));
        assertThrows(NullPointerException.class, () -> RoleName.from(null));
    }

    @Test
    void permissionNameShouldNormalizeAndValidate() {
        var permission = PermissionName.from(" report_read ");
        assertEquals("REPORT_READ", permission.value());

        assertThrows(IllegalArgumentException.class, () -> PermissionName.from("report-read"));
        assertThrows(NullPointerException.class, () -> PermissionName.from(null));
    }

    @Test
    void rawPasswordShouldRejectNullOrBlank() {
        var raw = RawPassword.from("Password@123");
        assertEquals("Password@123", raw.value());

        assertThrows(IllegalArgumentException.class, () -> RawPassword.from(""));
        assertThrows(IllegalArgumentException.class, () -> RawPassword.from("   "));
        assertThrows(IllegalArgumentException.class, () -> RawPassword.from(null));
    }

    @Test
    void encodedPasswordShouldRejectNullOrBlank() {
        var encoded = EncodedPassword.from("$2a$10$abc");
        assertEquals("$2a$10$abc", encoded.value());

        assertThrows(IllegalArgumentException.class, () -> EncodedPassword.from(""));
        assertThrows(IllegalArgumentException.class, () -> EncodedPassword.from("   "));
        assertThrows(IllegalArgumentException.class, () -> EncodedPassword.from(null));
    }
}
