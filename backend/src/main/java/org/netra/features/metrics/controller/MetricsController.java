package org.netra.features.metrics.controller;

import org.netra.features.metrics.dto.CommunityImpactDto;
import org.netra.features.metrics.dto.CommunityTimeSeriesDto;
import org.netra.features.metrics.service.CommunityMetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    /**
     * Returns real time-bucketed community impact metrics over the requested day window (7, 30, 90, 365 days).
     */
    @GetMapping("/community-impact/timeseries")
    public ResponseEntity<CommunityTimeSeriesDto> getCommunityImpactTimeSeries(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(communityMetricsService.getTimeSeries(days));
    }
}
