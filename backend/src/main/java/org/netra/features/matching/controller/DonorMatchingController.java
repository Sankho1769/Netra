package org.netra.features.matching.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.netra.core.security.ClientIpResolver;
import org.netra.core.security.SecurityUtils;
import org.netra.features.matching.dto.DonorMatchResponse;
import org.netra.features.matching.service.DonorMatchingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/blood-requests")
public class DonorMatchingController {

    private final DonorMatchingService donorMatchingService;
    private final ClientIpResolver clientIpResolver;

    public DonorMatchingController(
            DonorMatchingService donorMatchingService,
            ClientIpResolver clientIpResolver) {
        this.donorMatchingService = donorMatchingService;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping("/{requestId}/matches")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DonorMatchResponse> getMatches(
            @PathVariable("requestId") UUID requestId,
            @RequestParam(value = "radiusKm", required = false) Double radiusKm,
            @RequestParam(value = "limit", required = false) Integer limit,
            HttpServletRequest request) {

        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new org.netra.core.exception.UnauthorizedSessionAccessException("User is not authenticated."));

        String clientIp = clientIpResolver.resolveClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        DonorMatchResponse response = donorMatchingService.findMatches(
                requestId,
                currentUserId,
                radiusKm,
                limit,
                clientIp,
                userAgent
        );

        return ResponseEntity.ok(response);
    }
}
