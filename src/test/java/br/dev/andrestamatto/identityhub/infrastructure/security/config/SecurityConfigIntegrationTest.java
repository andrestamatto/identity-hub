package br.dev.andrestamatto.identityhub.infrastructure.security.config;

import br.dev.andrestamatto.identityhub.IdentityHubApplication;
import br.dev.andrestamatto.identityhub.application.ports.TokenServicePort;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {IdentityHubApplication.class, SecurityConfigIntegrationTest.TestEndpointsConfig.class})
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "identity-hub.security.rules[0].pattern=/test/permit",
        "identity-hub.security.rules[0].access=PERMIT_ALL",
        "identity-hub.security.rules[1].pattern=/test/deny",
        "identity-hub.security.rules[1].access=DENY_ALL",
        "identity-hub.security.rules[2].pattern=/test/auth",
        "identity-hub.security.rules[2].access=AUTHENTICATED",
        "identity-hub.security.rules[3].pattern=/test/any-role",
        "identity-hub.security.rules[3].access=ANY_ROLE:ADMIN,MANAGER",
        "identity-hub.security.rules[4].pattern=/test/all-perm",
        "identity-hub.security.rules[4].access=ALL_PERM:REPORT_READ,EXPORT_DATA",
        "identity-hub.security.rules[5].pattern=/test/ip",
        "identity-hub.security.rules[5].access=HAS_IP:127.0.0.1"
})
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TokenServicePort tokenServicePort;

    @Test
    void shouldAllowPermitAllWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/test/permit"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDenyDenyAll() throws Exception {
        mockMvc.perform(get("/test/deny"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRequireAuthenticationWhenRuleIsAuthenticated() throws Exception {
        mockMvc.perform(get("/test/auth"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAnyRoleWhenAtLeastOneRoleMatches() throws Exception {
        mockJwt("role-token", List.of("MANAGER"), List.of());
        mockMvc.perform(get("/test/any-role").header(HttpHeaders.AUTHORIZATION, "Bearer role-token"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDenyAnyRoleWhenNoRoleMatches() throws Exception {
        mockJwt("no-role-token", List.of("USER"), List.of());
        mockMvc.perform(get("/test/any-role").header(HttpHeaders.AUTHORIZATION, "Bearer no-role-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAllPermWhenAllPermissionsMatch() throws Exception {
        mockJwt("all-perm-token", List.of(), List.of("REPORT_READ", "EXPORT_DATA"));
        mockMvc.perform(get("/test/all-perm").header(HttpHeaders.AUTHORIZATION, "Bearer all-perm-token"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDenyAllPermWhenOnePermissionIsMissing() throws Exception {
        mockJwt("missing-perm-token", List.of(), List.of("REPORT_READ"));
        mockMvc.perform(get("/test/all-perm").header(HttpHeaders.AUTHORIZATION, "Bearer missing-perm-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowHasIpWhenIpMatches() throws Exception {
        mockMvc.perform(get("/test/ip").with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDenyHasIpWhenIpDoesNotMatch() throws Exception {
        mockMvc.perform(get("/test/ip").with(request -> {
                    request.setRemoteAddr("10.0.0.1");
                    return request;
                }))
                .andExpect(status().isForbidden());
    }

    private void mockJwt(String token, List<String> roles, List<String> permissions) {
        Claims claims = mock(Claims.class);
        when(tokenServicePort.isValid(token)).thenReturn(true);
        when(tokenServicePort.extractClaims(token)).thenReturn(claims);
        when(claims.getSubject()).thenReturn("user-subject");
        when(claims.get("roles", List.class)).thenReturn(roles);
        when(claims.get("permissions", List.class)).thenReturn(permissions);
    }

    @TestConfiguration
    static class TestEndpointsConfig {
        @Bean
        TestEndpointsController testEndpointsController() {
            return new TestEndpointsController();
        }
    }

    @RestController
    static class TestEndpointsController {
        @GetMapping("/test/permit")
        String permit() { return "ok"; }

        @GetMapping("/test/deny")
        String deny() { return "ok"; }

        @GetMapping("/test/auth")
        String auth() { return "ok"; }

        @GetMapping("/test/any-role")
        String anyRole() { return "ok"; }

        @GetMapping("/test/all-perm")
        String allPerm() { return "ok"; }

        @GetMapping("/test/ip")
        String hasIp() { return "ok"; }
    }
}
