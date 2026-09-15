package org.netra.features.donor.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.netra.core.security.ClientIpResolver;
import org.netra.features.donor.dto.CreateDonorProfileRequest;
import org.netra.features.donor.dto.DonorProfileDto;
import org.netra.features.donor.dto.UpdateDonorProfileRequest;
import org.netra.features.donor.service.DonorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/donor/profile")
public class DonorController {

    private final DonorService donorService;
    private final ClientIpResolver clientIpResolver;

    public DonorController(DonorService donorService, ClientIpResolver clientIpResolver) {
        this.donorService = donorService;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping
    public ResponseEntity<DonorProfileDto> getMyDonorProfile() {
        return ResponseEntity.ok(donorService.getDonorProfile());
    }

    @PostMapping
    public ResponseEntity<DonorProfileDto> createMyDonorProfile(
            @Valid @RequestBody CreateDonorProfileRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = clientIpResolver.resolveClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        DonorProfileDto created = donorService.createDonorProfile(request, clientIp, userAgent);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping
    public ResponseEntity<DonorProfileDto> updateMyDonorProfile(
            @Valid @RequestBody UpdateDonorProfileRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = clientIpResolver.resolveClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(donorService.updateDonorProfile(request, clientIp, userAgent));
    }
}
