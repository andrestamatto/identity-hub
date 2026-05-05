package br.dev.andrestamatto.identityhub.infrastructure.security.jwt;

import br.dev.andrestamatto.identityhub.application.ports.TokenServicePort;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSetAuthenticationWithRoleAndPermissionAuthorities() throws Exception {
        var tokenServicePort = mock(TokenServicePort.class);
        var claims = mock(Claims.class);
        var filter = new JwtAuthenticationFilter(tokenServicePort);

        when(tokenServicePort.isValid("valid-token")).thenReturn(true);
        when(tokenServicePort.extractClaims("valid-token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("user-subject");
        when(claims.get("roles", List.class)).thenReturn(List.of("ADMIN"));
        when(claims.get("permissions", List.class)).thenReturn(List.of("REPORT_READ"));

        var request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("user-subject", authentication.getName());
        var authorities = authentication.getAuthorities().stream().map(a -> a.getAuthority()).toList();
        assertTrue(authorities.contains("ROLE_ADMIN"));
        assertTrue(authorities.contains("PERM_REPORT_READ"));
    }

    @Test
    void shouldNotSetAuthenticationWhenTokenIsInvalid() throws Exception {
        var tokenServicePort = mock(TokenServicePort.class);
        var filter = new JwtAuthenticationFilter(tokenServicePort);

        when(tokenServicePort.isValid("invalid-token")).thenReturn(false);

        var request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid-token");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldNotSetAuthenticationWhenAuthorizationHeaderIsMissing() throws Exception {
        var tokenServicePort = mock(TokenServicePort.class);
        var filter = new JwtAuthenticationFilter(tokenServicePort);

        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(tokenServicePort);
    }
}
