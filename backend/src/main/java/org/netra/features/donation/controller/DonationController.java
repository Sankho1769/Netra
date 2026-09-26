package org.netra.features.donation.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.netra.core.exception.UnauthorizedSessionAccessException;
import org.netra.core.security.ClientIpResolver;
import org.netra.core.security.SecurityUtils;
import org.netra.features.donation.dto.*;
import org.netra.features.donation.service.DonationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/donations")
public class DonationController {

    private final DonationService donationService;
    private final ClientIpResolver clientIpResolver;

    public DonationController(DonationService donationService, ClientIpResolver clientIpResolver) {
        this.donationService = donationService;
        this.clientIpResolver = clientIpResolver;
    }

    /**
     * Submit a donation claim (by authenticated donor).
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DonationDetailDto> createClaim(
            @Valid @RequestBody CreateDonationClaimRequest request,
            HttpServletRequest servletRequest) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User is not authenticated."));

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        DonationDetailDto dto = donationService.createClaim(currentUserId, request, clientIp, userAgent);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Get the authenticated donor's donation history.
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<DonationDto>> getMyDonations(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User is not authenticated."));

        int boundedSize = Math.max(1, Math.min(size, 50));
        Pageable pageable = PageRequest.of(Math.max(0, page), boundedSize);

        Page<DonationDto> donations = donationService.getMyDonations(currentUserId, pageable);
        return ResponseEntity.ok(donations);
    }

    /**
     * Get single donation detail.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DonationDetailDto> getDonationDetail(@PathVariable("id") UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User is not authenticated."));

        DonationDetailDto dto = donationService.getDonationDetail(id, currentUserId);
        return ResponseEntity.ok(dto);
    }

    /**
     * Cancel a pending donation claim (by claiming donor).
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DonationDetailDto> cancelClaim(
            @PathVariable("id") UUID id,
            HttpServletRequest servletRequest) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User is not authenticated."));

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        DonationDetailDto dto = donationService.cancelClaim(id, currentUserId, clientIp, userAgent);
        return ResponseEntity.ok(dto);
    }

    /**
     * List pending verification queue for authorized staff.
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('BLOODBANK', 'ORGANIZATION', 'ADMIN')")
    public ResponseEntity<Page<DonationDetailDto>> getPendingDonations(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        int boundedSize = Math.max(1, Math.min(size, 50));
        Pageable pageable = PageRequest.of(Math.max(0, page), boundedSize);

        Page<DonationDetailDto> pending = donationService.getPendingDonations(pageable);
        return ResponseEntity.ok(pending);
    }

    /**
     * Verify a donation claim (by authorized staff).
     */
    @PostMapping("/{id}/verify")
    @PreAuthorize("hasAnyRole('BLOODBANK', 'ORGANIZATION', 'ADMIN')")
    public ResponseEntity<DonationDetailDto> verifyDonation(
            @PathVariable("id") UUID id,
            @Valid @RequestBody(required = false) VerifyDonationRequest request,
            HttpServletRequest servletRequest) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User is not authenticated."));

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        DonationDetailDto dto = donationService.verifyDonation(id, currentUserId, request, clientIp, userAgent);
        return ResponseEntity.ok(dto);
    }

    /**
     * Reject a donation claim (by authorized staff).
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('BLOODBANK', 'ORGANIZATION', 'ADMIN')")
    public ResponseEntity<DonationDetailDto> rejectDonation(
            @PathVariable("id") UUID id,
            @Valid @RequestBody RejectDonationRequest request,
            HttpServletRequest servletRequest) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User is not authenticated."));

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        DonationDetailDto dto = donationService.rejectDonation(id, currentUserId, request, clientIp, userAgent);
        return ResponseEntity.ok(dto);
    }

    /**
     * Directly record and verify an in-person donation (by authorized staff).
     */
    @PostMapping("/record-verified")
    @PreAuthorize("hasAnyRole('BLOODBANK', 'ORGANIZATION', 'ADMIN')")
    public ResponseEntity<DonationDetailDto> recordVerifiedDonation(
            @Valid @RequestBody RecordVerifiedDonationRequest request,
            HttpServletRequest servletRequest) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User is not authenticated."));

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        DonationDetailDto dto = donationService.recordVerifiedDonation(currentUserId, request, clientIp, userAgent);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Admin correction endpoint.
     */
    @PostMapping("/{id}/admin-correction")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DonationDetailDto> adminCorrection(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AdminCorrectionRequest request,
            HttpServletRequest servletRequest) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User is not authenticated."));

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        DonationDetailDto dto = donationService.adminCorrection(id, currentUserId, request, clientIp, userAgent);
        return ResponseEntity.ok(dto);
    }
}
