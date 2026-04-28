package br.dev.andrestamatto.identityhub.infrastructure.security.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfiguredAccessRuleTest {

    @Test
    void shouldReturnEmptyWhenRuleIsNullOrBlank() {
        assertTrue(ConfiguredAccessRule.from(null).isEmpty());
        assertTrue(ConfiguredAccessRule.from(new IdentityHubSecurityProperties.Rule(" ", "AUTHENTICATED")).isEmpty());
        assertTrue(ConfiguredAccessRule.from(new IdentityHubSecurityProperties.Rule("/users/**", " ")).isEmpty());
    }

    @Test
    void shouldParseSingleValueRules() {
        var roleRule = ConfiguredAccessRule.from(new IdentityHubSecurityProperties.Rule("/admin/**", "ROLE:admin")).orElseThrow();
        assertEquals(AccessType.ROLE, roleRule.type());
        assertArrayEquals(new String[]{"ADMIN"}, roleRule.values());

        var permRule = ConfiguredAccessRule.from(new IdentityHubSecurityProperties.Rule("/reports/**", "PERM:report_read")).orElseThrow();
        assertEquals(AccessType.PERM, permRule.type());
        assertArrayEquals(new String[]{"REPORT_READ"}, permRule.values());
    }

    @Test
    void shouldParseMultiValueRules() {
        var anyRole = ConfiguredAccessRule.from(new IdentityHubSecurityProperties.Rule("/x", "ANY_ROLE:admin, manager")).orElseThrow();
        assertEquals(AccessType.ANY_ROLE, anyRole.type());
        assertArrayEquals(new String[]{"ADMIN", "MANAGER"}, anyRole.values());

        var allPerm = ConfiguredAccessRule.from(new IdentityHubSecurityProperties.Rule("/y", "ALL_PERM:read,write")).orElseThrow();
        assertEquals(AccessType.ALL_PERM, allPerm.type());
        assertArrayEquals(new String[]{"READ", "WRITE"}, allPerm.values());
    }

    @Test
    void shouldParseHasIpWithoutUpperCasing() {
        var hasIp = ConfiguredAccessRule.from(new IdentityHubSecurityProperties.Rule("/internal/**", "HAS_IP:127.0.0.1, 10.0.0.8")).orElseThrow();
        assertEquals(AccessType.HAS_IP, hasIp.type());
        assertArrayEquals(new String[]{"127.0.0.1", "10.0.0.8"}, hasIp.values());
    }

    @Test
    void shouldThrowForUnsupportedRule() {
        var ex = assertThrows(IllegalStateException.class,
                () -> ConfiguredAccessRule.from(new IdentityHubSecurityProperties.Rule("/z", "SOMETHING:ABC")));
        assertTrue(ex.getMessage().contains("Unsupported access rule"));
    }
}
