package org.netra.features.events.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.netra.core.security.ClientIpResolver;
import org.netra.features.events.dto.*;
import org.netra.features.events.entity.DonationEventStatus;
import org.netra.features.events.service.DonationEventService;
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
@RequestMapping("/api/v1/donation-events")
public class DonationEventController {

    private final DonationEventService donationEventService;
    private final ClientIpResolver clientIpResolver;

    public DonationEventController(
            DonationEventService donationEventService,
            ClientIpResolver clientIpResolver) {
        this.donationEventService = donationEventService;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping
    public ResponseEntity<Page<DonationEventSummaryDto>> discoverEvents(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) UUID bloodBankId,
            @RequestParam(required = false) DonationEventStatus status,
            @RequestParam(required = false, defaultValue = "false") Boolean upcomingOnly,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<DonationEventSummaryDto> results = donationEventService.discoverEvents(
                city, bloodBankId, status, upcomingOnly, pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<DonationEventSummaryDto>> findNearbyEvents(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "10.0") Double radiusKm) {

        List<DonationEventSummaryDto> results = donationEventService.findNearbyEvents(
                latitude, longitude, radiusKm);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DonationEventDetailDto> getEventById(@PathVariable UUID id) {
        DonationEventDetailDto dto = donationEventService.getEventById(id);
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOODBANK')")
    public ResponseEntity<DonationEventDetailDto> createEvent(
            @Valid @RequestBody CreateDonationEventRequest request,
            HttpServletRequest servletRequest) {

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        DonationEventDetailDto created = donationEventService.createEvent(request, clientIp, userAgent);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOODBANK')")
    public ResponseEntity<DonationEventDetailDto> updateEvent(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDonationEventRequest request,
            HttpServletRequest servletRequest) {

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        DonationEventDetailDto updated = donationEventService.updateEvent(id, request, clientIp, userAgent);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOODBANK')")
    public ResponseEntity<DonationEventDetailDto> submitForApproval(
            @PathVariable UUID id,
            HttpServletRequest servletRequest) {

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        DonationEventDetailDto submitted = donationEventService.submitForApproval(id, clientIp, userAgent);
        return ResponseEntity.ok(submitted);
    }

    @PatchMapping("/{id}/approval")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DonationEventDetailDto> reviewEventApproval(
            @PathVariable UUID id,
            @Valid @RequestBody ApproveDonationEventRequest request,
            HttpServletRequest servletRequest) {

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        DonationEventDetailDto reviewed = donationEventService.reviewEventApproval(id, request, clientIp, userAgent);
        return ResponseEntity.ok(reviewed);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOODBANK')")
    public ResponseEntity<DonationEventDetailDto> cancelEvent(
            @PathVariable UUID id,
            @RequestBody(required = false) CancelDonationEventRequest request,
            HttpServletRequest servletRequest) {

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        DonationEventDetailDto cancelled = donationEventService.cancelEvent(id, request, clientIp, userAgent);
        return ResponseEntity.ok(cancelled);
    }
}
