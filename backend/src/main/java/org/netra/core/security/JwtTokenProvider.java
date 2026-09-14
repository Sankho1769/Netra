package org.netra.core.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final String DEFAULT_DEV_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    private final SecretKey secretKey;
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;

    public JwtTokenProvider(
            @Value("${netra.security.jwt.secret:#{null}}") String secret,
            @Value("${netra.security.jwt.access-token-validity-ms:3600000}") long accessTokenValidityMs,
            @Value("${netra.security.jwt.refresh-token-validity-ms:604800000}") long refreshTokenValidityMs,
            Environment environment) {

        boolean isProduction = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> "prod".equalsIgnoreCase(p) || "production".equalsIgnoreCase(p));

        if (isProduction && (secret == null || secret.isBlank() || DEFAULT_DEV_SECRET.equalsIgnoreCase(secret.trim()))) {
            throw new IllegalStateException("FATAL: Production JWT Secret is missing or using default development secret. NETRA requires a strong, externally configured JWT secret in production.");
        }

        String effectiveSecret = (secret != null && !secret.isBlank()) ? secret.trim() : DEFAULT_DEV_SECRET;
        byte[] secretBytes = effectiveSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("FATAL: JWT Secret Key must be at least 256 bits (32 bytes). Current key has " + secretBytes.length + " bytes.");
        }

        this.secretKey = Keys.hmacShaKeyFor(secretBytes);
        this.accessTokenValidityMs = accessTokenValidityMs;
        this.refreshTokenValidityMs = refreshTokenValidityMs;
    }

    public String generateAccessToken(UUID userId, java.util.Collection<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenValidityMs);

        String rolesString = (roles != null && !roles.isEmpty()) ? String.join(",", roles) : "ROLE_DONOR";
        String primaryRole = (roles != null && !roles.isEmpty()) ? roles.iterator().next() : "ROLE_DONOR";

        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "ACCESS")
                .claim("roles", rolesString)
                .claim("role", primaryRole)
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public String generateAccessToken(UUID userId, String role) {
        return generateAccessToken(userId, Collections.singletonList(role));
    }

    public String generateRefreshToken(UUID userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenValidityMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "REFRESH")
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public String getTokenType(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
            return claims.get("type", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    public UUID getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
            return UUID.fromString(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }

    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
        String userId = claims.getSubject();
        String rolesStr = claims.get("roles", String.class);

        List<SimpleGrantedAuthority> authorities;
        if (rolesStr != null && !rolesStr.isBlank()) {
            authorities = Arrays.stream(rolesStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        } else {
            String role = claims.get("role", String.class);
            if (role == null) {
                role = "ROLE_DONOR";
            }
            authorities = Collections.singletonList(new SimpleGrantedAuthority(role));
        }

        User principal = new User(userId, "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }
}
