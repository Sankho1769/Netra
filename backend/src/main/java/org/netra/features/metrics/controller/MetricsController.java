package org.netra.features.metrics.controller;

import org.netra.features.metrics.dto.CommunityImpactDto;
import org.netra.features.metrics.service.CommunityMetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/metrics")
public class MetricsController {

    private final CommunityMetricsService communityMetricsService;

    public MetricsController(CommunityMetricsService communityMetricsService) {
        this.communityMetricsService = communityMetricsService;
    }

    /**
     * Returns authoritative community impact metrics derived from verified records.
     */
    @GetMapping("/community-impact")
    public ResponseEntity<CommunityImpactDto> getCommunityImpact() {
        return ResponseEntity.ok(communityMetricsService.getCommunityImpact());
    }
}
