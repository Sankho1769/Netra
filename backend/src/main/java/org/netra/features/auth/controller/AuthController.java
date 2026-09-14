package org.netra.features.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.netra.core.exception.UnauthorizedSessionAccessException;
import org.netra.core.ratelimit.RateLimitingService;
import org.netra.core.security.ClientIpResolver;
import org.netra.core.security.SecurityUtils;
import org.netra.features.auth.dto.*;
import org.netra.features.auth.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final RateLimitingService rateLimitingService;
    private final ClientIpResolver clientIpResolver;

    public AuthController(
            AuthService authService,
            RateLimitingService rateLimitingService,
            ClientIpResolver clientIpResolver) {
        this.authService = authService;
        this.rateLimitingService = rateLimitingService;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest) {

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        rateLimitingService.checkRateLimit(clientIp);

        AuthResponse response = authService.register(
                request,
                clientIp,
                servletRequest.getHeader("User-Agent")
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        rateLimitingService.checkRateLimit(clientIp);

        AuthResponse response = authService.login(
                request,
                clientIp,
                servletRequest.getHeader("User-Agent")
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody TokenRefreshRequest request,
            HttpServletRequest servletRequest) {

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        rateLimitingService.checkRateLimit(clientIp);

        AuthResponse response = authService.refresh(
                request,
                clientIp,
                servletRequest.getHeader("User-Agent")
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestBody(required = false) LogoutRequest request,
            HttpServletRequest servletRequest) {

        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User is not authenticated."));

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        authService.logout(request, currentUserId, clientIp, servletRequest.getHeader("User-Agent"));

        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Logged out successfully"));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, String>> logoutAll(HttpServletRequest servletRequest) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User is not authenticated."));

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        authService.logoutAll(currentUserId, clientIp, servletRequest.getHeader("User-Agent"));

        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "All sessions terminated successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserSummaryDto> getMe() {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User is not authenticated."));

        return ResponseEntity.ok(authService.getCurrentUserSummary(currentUserId));
    }
}
