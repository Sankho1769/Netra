package org.netra.features.bloodbank.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.netra.core.security.ClientIpResolver;
import org.netra.features.bloodbank.dto.*;
import org.netra.features.bloodbank.entity.BloodBankOperatingStatus;
import org.netra.features.bloodbank.service.BloodBankService;
import org.netra.features.bloodbank.service.BloodInventoryService;
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
@RequestMapping("/api/v1/bloodbanks")
public class BloodBankController {

    private final BloodBankService bloodBankService;
    private final BloodInventoryService bloodInventoryService;
    private final ClientIpResolver clientIpResolver;

    public BloodBankController(
            BloodBankService bloodBankService,
            BloodInventoryService bloodInventoryService,
            ClientIpResolver clientIpResolver) {
        this.bloodBankService = bloodBankService;
        this.bloodInventoryService = bloodInventoryService;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping
    public ResponseEntity<Page<BloodBankSummaryDto>> discoverBloodBanks(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) BloodGroup bloodGroup,
            @RequestParam(required = false) BloodBankOperatingStatus operatingStatus,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<BloodBankSummaryDto> result = bloodBankService.discoverBloodBanks(
                city, bloodGroup, operatingStatus, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<BloodBankSummaryDto>> findNearbyBloodBanks(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "10.0") Double radiusKm) {

        List<BloodBankSummaryDto> result = bloodBankService.findNearbyBloodBanks(
                latitude, longitude, radiusKm);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BloodBankDetailDto> getBloodBankById(
            @PathVariable UUID id,
            @RequestParam(required = false) Double userLat,
            @RequestParam(required = false) Double userLon) {

        BloodBankDetailDto dto = bloodBankService.getBloodBankById(id, userLat, userLon);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}/inventory")
    public ResponseEntity<List<BloodInventoryDto>> getInventory(@PathVariable UUID id) {
        List<BloodInventoryDto> inventory = bloodInventoryService.getInventory(id);
        return ResponseEntity.ok(inventory);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BloodBankDetailDto> createBloodBank(
            @Valid @RequestBody CreateBloodBankRequest request,
            HttpServletRequest servletRequest) {

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        BloodBankDetailDto created = bloodBankService.createBloodBank(request, clientIp, userAgent);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOODBANK')")
    public ResponseEntity<BloodBankDetailDto> updateBloodBank(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBloodBankRequest request,
            HttpServletRequest servletRequest) {

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        BloodBankDetailDto updated = bloodBankService.updateBloodBank(id, request, clientIp, userAgent);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/verification")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BloodBankDetailDto> transitionVerificationStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBloodBankVerificationRequest request,
            HttpServletRequest servletRequest) {

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        BloodBankDetailDto updated = bloodBankService.transitionVerificationStatus(
                id, request.getStatus(), clientIp, userAgent);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOODBANK')")
    public ResponseEntity<BloodBankDetailDto> updateOperatingStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOperatingStatusRequest request,
            HttpServletRequest servletRequest) {

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        BloodBankDetailDto updated = bloodBankService.updateOperatingStatus(
                id, request.getOperatingStatus(), clientIp, userAgent);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/inventory")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOODBANK')")
    public ResponseEntity<BloodInventoryDto> updateInventory(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInventoryRequest request,
            HttpServletRequest servletRequest) {

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        BloodInventoryDto updated = bloodInventoryService.updateInventory(id, request, clientIp, userAgent);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/accounts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BloodBankAccountDto> linkStaffAccount(
            @PathVariable UUID id,
            @Valid @RequestBody LinkStaffAccountRequest request,
            HttpServletRequest servletRequest) {

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        BloodBankAccountDto account = bloodBankService.linkStaffAccount(
                id, request.getUserId(), clientIp, userAgent);
        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }

    @PatchMapping("/{id}/accounts/{accountId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BloodBankAccountDto> updateStaffAccountStatus(
            @PathVariable UUID id,
            @PathVariable UUID accountId,
            @Valid @RequestBody UpdateStaffAccountStatusRequest request,
            HttpServletRequest servletRequest) {

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        BloodBankAccountDto account = bloodBankService.updateStaffAccountStatus(
                id, accountId, request.getStatus(), clientIp, userAgent);
        return ResponseEntity.ok(account);
    }
}
