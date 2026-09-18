package org.netra.features.emergency.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.netra.core.exception.ValidationException;
import org.netra.core.security.ClientIpResolver;
import org.netra.features.bloodrequest.dto.BloodRequestDetailDto;
import org.netra.features.bloodrequest.dto.CancelBloodRequestRequest;
import org.netra.features.emergency.dto.EmergencyBloodRequestRequest;
import org.netra.features.emergency.dto.EmergencyCreationResult;
import org.netra.features.emergency.service.EmergencyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/emergency")
public class EmergencyController {

    private final EmergencyService emergencyService;
    private final ClientIpResolver clientIpResolver;

    public EmergencyController(
            EmergencyService emergencyService,
            ClientIpResolver clientIpResolver) {
        this.emergencyService = emergencyService;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/blood-requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BloodRequestDetailDto> createEmergencyRequest(
            @Valid @RequestBody EmergencyBloodRequestRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest servletRequest) {

        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            throw new ValidationException("Idempotency-Key header is required.");
        }
        if (idempotencyKey.trim().length() > 255) {
            throw new ValidationException("Idempotency-Key header must not exceed 255 characters.");
        }

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        EmergencyCreationResult result = emergencyService.createEmergencyRequest(
                request, idempotencyKey, clientIp, userAgent);

        if (result.isReplay()) {
            return ResponseEntity.ok(result.detail());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result.detail());
    }

    @PostMapping("/blood-requests/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BloodRequestDetailDto> cancelEmergencyRequest(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) CancelBloodRequestRequest request,
            HttpServletRequest servletRequest) {

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        BloodRequestDetailDto cancelled = emergencyService.cancelEmergencyRequest(
                id, request, clientIp, userAgent);
        return ResponseEntity.ok(cancelled);
    }
}
