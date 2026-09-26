package org.netra.features.donor.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.netra.core.security.ClientIpResolver;
import org.netra.features.donor.dto.CreateDonorProfileRequest;
import org.netra.features.donor.dto.DonorProfileDto;
import org.netra.features.donor.dto.UpdateDonorProfileRequest;
import org.netra.features.donor.dto.VerifyDonorBloodGroupRequest;
import org.netra.features.donor.service.DonorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class DonorController {

    private final DonorService donorService;
    private final ClientIpResolver clientIpResolver;

    public DonorController(DonorService donorService, ClientIpResolver clientIpResolver) {
        this.donorService = donorService;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping("/api/v1/donor/profile")
    public ResponseEntity<DonorProfileDto> getMyDonorProfile() {
        return ResponseEntity.ok(donorService.getDonorProfile());
    }

    @PostMapping("/api/v1/donor/profile")
    public ResponseEntity<DonorProfileDto> createMyDonorProfile(
            @Valid @RequestBody CreateDonorProfileRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = clientIpResolver.resolveClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        DonorProfileDto created = donorService.createDonorProfile(request, clientIp, userAgent);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/api/v1/donor/profile")
    public ResponseEntity<DonorProfileDto> updateMyDonorProfile(
            @Valid @RequestBody UpdateDonorProfileRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = clientIpResolver.resolveClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(donorService.updateDonorProfile(request, clientIp, userAgent));
    }

    @PostMapping({"/api/v1/donors/{userId}/verify-blood-group", "/api/v1/donor/profile/{userId}/verify-blood-group"})
    @PreAuthorize("hasAnyRole('BLOODBANK', 'ADMIN')")
    public ResponseEntity<DonorProfileDto> verifyDonorBloodGroup(
            @PathVariable UUID userId,
            @Valid @RequestBody(required = false) VerifyDonorBloodGroupRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = clientIpResolver.resolveClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        DonorProfileDto verified = donorService.verifyDonorBloodGroup(userId, request, clientIp, userAgent);
        return ResponseEntity.ok(verified);
    }
}
