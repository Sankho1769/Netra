package org.netra.core.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("hasRole: returns true regardless of role ordering or ROLE_ prefix")
    void testHasRoleWithMultipleRoles() {
        UUID userId = UUID.randomUUID();
        // Authorities list with DONOR first, then ADMIN
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_DONOR"),
                new SimpleGrantedAuthority("ROLE_ADMIN")
        );
        User principal = new User(userId.toString(), "pass", authorities);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "token", authorities)
        );

        // Verify hasRole with and without prefix
        assertTrue(SecurityUtils.hasRole("ROLE_ADMIN"));
        assertTrue(SecurityUtils.hasRole("ADMIN"));
        assertTrue(SecurityUtils.hasRole("ROLE_DONOR"));
        assertTrue(SecurityUtils.hasRole("DONOR"));

        // Verify hasRole for roles not present
        assertFalse(SecurityUtils.hasRole("BLOODBANK"));
        assertFalse(SecurityUtils.hasRole("ROLE_RECEIVER"));
    }

    @Test
    @DisplayName("hasAnyRole: returns true if any requested role matches regardless of authority ordering")
    void testHasAnyRoleWithMultipleRoles() {
        UUID userId = UUID.randomUUID();
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_RECEIVER"),
                new SimpleGrantedAuthority("ROLE_BLOODBANK")
        );
        User principal = new User(userId.toString(), "pass", authorities);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "token", authorities)
        );

        assertTrue(SecurityUtils.hasAnyRole("ADMIN", "BLOODBANK"));
        assertTrue(SecurityUtils.hasAnyRole("ROLE_ADMIN", "ROLE_RECEIVER"));
        assertFalse(SecurityUtils.hasAnyRole("ADMIN", "DONOR"));
    }

    @Test
    @DisplayName("getCurrentUserAuthorities: returns all granted authorities as a Set")
    void testGetCurrentUserAuthorities() {
        UUID userId = UUID.randomUUID();
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_DONOR"),
                new SimpleGrantedAuthority("ROLE_RECEIVER"),
                new SimpleGrantedAuthority("ROLE_ADMIN")
        );
        User principal = new User(userId.toString(), "pass", authorities);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "token", authorities)
        );

        Set<String> authSet = SecurityUtils.getCurrentUserAuthorities();
        assertEquals(3, authSet.size());
        assertTrue(authSet.contains("ROLE_DONOR"));
        assertTrue(authSet.contains("ROLE_RECEIVER"));
        assertTrue(authSet.contains("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("Unauthenticated or anonymous context: hasRole and hasAnyRole return false gracefully")
    void testUnauthenticatedAndAnonymousHandling() {
        assertFalse(SecurityUtils.hasRole("ADMIN"));
        assertFalse(SecurityUtils.hasAnyRole("ADMIN", "DONOR"));
        assertTrue(SecurityUtils.getCurrentUserAuthorities().isEmpty());
        assertTrue(SecurityUtils.getCurrentUserId().isEmpty());

        // Anonymous auth token
        AnonymousAuthenticationToken anonToken = new AnonymousAuthenticationToken(
                "key", "anonymousUser", Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        SecurityContextHolder.getContext().setAuthentication(anonToken);

        assertFalse(SecurityUtils.hasRole("ROLE_ANONYMOUS"));
        assertFalse(SecurityUtils.hasAnyRole("ROLE_ANONYMOUS"));
        assertTrue(SecurityUtils.getCurrentUserId().isEmpty());
    }

    @Test
    @DisplayName("Token generation and SHA-256 hashing produce expected secure formats")
    void testTokenGenerationAndHashing() {
        String token = SecurityUtils.generateSecureToken();
        assertNotNull(token);
        assertEquals(64, token.length()); // 32 bytes hex encoded = 64 chars

        String hash = SecurityUtils.sha256Hex("test-password-or-token");
        assertNotNull(hash);
        assertEquals(64, hash.length());
        assertEquals(hash, SecurityUtils.sha256Hex("test-password-or-token")); // deterministic
        assertNull(SecurityUtils.sha256Hex(null));
    }
}
