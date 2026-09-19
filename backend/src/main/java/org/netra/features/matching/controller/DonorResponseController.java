package org.netra.features.matching.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.netra.core.exception.UnauthorizedSessionAccessException;
import org.netra.core.security.ClientIpResolver;
import org.netra.core.security.SecurityUtils;
import org.netra.features.matching.dto.DonorMatchDetailDto;
import org.netra.features.matching.service.DonorResponseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for donor-facing match endpoints.
 *
 * All operations strictly authenticate the donor via the SecurityContext.
 */
@RestController
@RequestMapping("/api/v1/donor/matches")
public class DonorResponseController {

    private final DonorResponseService donorResponseService;
    private final ClientIpResolver clientIpResolver;

    public DonorResponseController(
            DonorResponseService donorResponseService,
            ClientIpResolver clientIpResolver) {
        this.donorResponseService = donorResponseService;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DonorMatchDetailDto>> getMyMatches() {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User is not authenticated."));

        List<DonorMatchDetailDto> matches = donorResponseService.getMatchesForDonor(currentUserId);
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/{matchId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DonorMatchDetailDto> getMatchDetail(@PathVariable("matchId") UUID matchId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User is not authenticated."));

        DonorMatchDetailDto match = donorResponseService.getMatchForDonor(matchId, currentUserId);
        return ResponseEntity.ok(match);
    }

    @PostMapping("/{matchId}/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DonorMatchDetailDto> acceptMatch(
            @PathVariable("matchId") UUID matchId,
            HttpServletRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User is not authenticated."));

        String clientIp = clientIpResolver.resolveClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        DonorMatchDetailDto updated = donorResponseService.acceptMatch(matchId, currentUserId, clientIp, userAgent);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{matchId}/decline")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DonorMatchDetailDto> declineMatch(
            @PathVariable("matchId") UUID matchId,
            HttpServletRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User is not authenticated."));

        String clientIp = clientIpResolver.resolveClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        DonorMatchDetailDto updated = donorResponseService.declineMatch(matchId, currentUserId, clientIp, userAgent);
        return ResponseEntity.ok(updated);
    }
}
