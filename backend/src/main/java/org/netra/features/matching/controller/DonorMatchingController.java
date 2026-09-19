package org.netra.features.matching.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.netra.core.exception.UnauthorizedSessionAccessException;
import org.netra.core.security.ClientIpResolver;
import org.netra.core.security.SecurityUtils;
import org.netra.features.matching.dto.CreateDonorMatchRequest;
import org.netra.features.matching.dto.DonorMatchResponse;
import org.netra.features.matching.dto.RequesterDonorMatchDto;
import org.netra.features.matching.service.DonorMatchingService;
import org.netra.features.matching.service.DonorResponseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller managing blood request matching operations.
 *
 * Endpoints:
 * - GET  /api/v1/blood-requests/{requestId}/matches: Computed candidate search (read-only)
 * - POST /api/v1/blood-requests/{requestId}/matches: Creates persistent donor match
 * - GET  /api/v1/blood-requests/{requestId}/match-responses: Retrieves persistent matches for request
 */
@RestController
@RequestMapping("/api/v1/blood-requests")
public class DonorMatchingController {

    private final DonorMatchingService donorMatchingService;
    private final DonorResponseService donorResponseService;
    private final ClientIpResolver clientIpResolver;
    private final org.netra.core.ratelimit.RateLimitingService rateLimitingService;

    public DonorMatchingController(
            DonorMatchingService donorMatchingService,
            DonorResponseService donorResponseService,
            ClientIpResolver clientIpResolver,
            org.netra.core.ratelimit.RateLimitingService rateLimitingService) {
        this.donorMatchingService = donorMatchingService;
        this.donorResponseService = donorResponseService;
        this.clientIpResolver = clientIpResolver;
        this.rateLimitingService = rateLimitingService;
    }

    /**
     * Computes candidate donor matches for the specified blood request.
     * Pure read operation.
     */
    @GetMapping("/{requestId}/matches")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DonorMatchResponse> getMatches(
            @PathVariable("requestId") UUID requestId,
            @RequestParam(value = "radiusKm", required = false) Double radiusKm,
            @RequestParam(value = "limit", required = false) Integer limit,
            HttpServletRequest request) {

        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User is not authenticated."));

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

    /**
     * Explicitly creates a persistent donor match record for the intended candidate.
     */
    @PostMapping("/{requestId}/matches")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RequesterDonorMatchDto> createMatch(
            @PathVariable("requestId") UUID requestId,
            @Valid @RequestBody CreateDonorMatchRequest matchRequest,
            HttpServletRequest request) {

        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User is not authenticated."));

        rateLimitingService.checkMatchCreationRateLimit(currentUserId.toString());

        String clientIp = clientIpResolver.resolveClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        RequesterDonorMatchDto created = donorResponseService.createMatch(
                requestId,
                matchRequest,
                currentUserId,
                clientIp,
                userAgent
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Retrieves persistent donor match records associated with the specified blood request.
     */
    @GetMapping("/{requestId}/match-responses")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RequesterDonorMatchDto>> getMatchResponses(
            @PathVariable("requestId") UUID requestId) {

        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User is not authenticated."));

        List<RequesterDonorMatchDto> responses = donorResponseService.getMatchesForRequest(
                requestId,
                currentUserId
        );

        return ResponseEntity.ok(responses);
    }
}
