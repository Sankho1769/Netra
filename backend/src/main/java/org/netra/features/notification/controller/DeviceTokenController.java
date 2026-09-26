package org.netra.features.notification.controller;

import jakarta.validation.Valid;
import org.netra.core.exception.UnauthorizedSessionAccessException;
import org.netra.core.security.SecurityUtils;
import org.netra.features.notification.dto.DeviceTokenDto;
import org.netra.features.notification.dto.RegisterDeviceTokenRequest;
import org.netra.features.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/devices/tokens")
public class DeviceTokenController {

    private final NotificationService notificationService;

    public DeviceTokenController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DeviceTokenDto> registerToken(@Valid @RequestBody RegisterDeviceTokenRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User is not authenticated."));

        DeviceTokenDto dto = notificationService.registerDeviceToken(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> revokeToken(@PathVariable("id") UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User is not authenticated."));

        notificationService.revokeDeviceToken(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/revoke")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> revokeTokenByValue(@Valid @RequestBody org.netra.features.notification.dto.RevokeDeviceTokenRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User is not authenticated."));

        notificationService.revokeDeviceTokenByToken(request.getToken(), currentUserId);
        return ResponseEntity.noContent().build();
    }
}
