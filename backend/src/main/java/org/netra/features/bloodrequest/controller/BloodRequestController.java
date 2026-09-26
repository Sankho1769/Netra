package org.netra.features.bloodrequest.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.netra.core.security.ClientIpResolver;
import org.netra.features.bloodrequest.dto.*;
import org.netra.features.bloodrequest.entity.BloodRequestUrgency;
import org.netra.features.bloodrequest.service.BloodRequestService;
import org.netra.features.donor.entity.BloodGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/blood-requests")
public class BloodRequestController {

    private final BloodRequestService bloodRequestService;
    private final ClientIpResolver clientIpResolver;

    public BloodRequestController(
            BloodRequestService bloodRequestService,
            ClientIpResolver clientIpResolver) {
        this.bloodRequestService = bloodRequestService;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BloodRequestDetailDto> createRequest(
            @Valid @RequestBody CreateBloodRequestRequest request,
            HttpServletRequest servletRequest) {

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        BloodRequestDetailDto created = bloodRequestService.createRequest(request, clientIp, userAgent);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<Page<BloodRequestSummaryDto>> discoverRequests(
            @RequestParam(required = false) BloodGroup bloodGroup,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) BloodRequestUrgency urgency,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<BloodRequestSummaryDto> results = bloodRequestService.discoverRequests(
                bloodGroup, city, urgency, pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<BloodRequestSummaryDto>> getMyRequests(
            @PageableDefault(size = 20) Pageable pageable) {

        Page<BloodRequestSummaryDto> results = bloodRequestService.getMyRequests(pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<BloodRequestSummaryDto>> findNearbyRequests(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "10.0") Double radiusKm,
            @RequestParam(required = false) BloodGroup bloodGroup) {

        List<BloodRequestSummaryDto> results = bloodRequestService.findNearbyRequests(
                latitude, longitude, radiusKm, bloodGroup);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BloodRequestPublicDetailDto> getRequestById(@PathVariable UUID id) {
        BloodRequestPublicDetailDto dto = bloodRequestService.getRequestById(id);
        return ResponseEntity.ok(dto);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BloodRequestDetailDto> updateRequest(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBloodRequestRequest request,
            HttpServletRequest servletRequest) {

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        BloodRequestDetailDto updated = bloodRequestService.updateRequest(id, request, clientIp, userAgent);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BloodRequestDetailDto> cancelRequest(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) CancelBloodRequestRequest request,
            HttpServletRequest servletRequest) {

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        BloodRequestDetailDto cancelled = bloodRequestService.cancelRequest(id, request, clientIp, userAgent);
        return ResponseEntity.ok(cancelled);
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasAnyRole('BLOODBANK', 'ADMIN')")
    public ResponseEntity<BloodRequestDetailDto> verifyRequest(
            @PathVariable UUID id,
            @Valid @RequestBody VerifyBloodRequestDto request,
            HttpServletRequest servletRequest) {

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        BloodRequestDetailDto verified = bloodRequestService.verifyRequest(id, request, clientIp, userAgent);
        return ResponseEntity.ok(verified);
    }
}
