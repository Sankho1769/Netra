package org.netra.features.matching.service;

import org.netra.core.audit.AuditService;
import org.netra.core.exception.ResourceNotFoundException;
import org.netra.core.exception.ValidationException;
import org.netra.core.ratelimit.RateLimitingService;
import org.netra.features.bloodbank.service.BloodBankService;
import org.netra.features.bloodrequest.entity.BloodRequest;
import org.netra.features.bloodrequest.entity.BloodRequestStatus;
import org.netra.features.bloodrequest.repository.BloodRequestRepository;
import org.netra.features.bloodrequest.service.BloodRequestAuthorizationService;
import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.donor.entity.BloodGroupVerificationStatus;
import org.netra.features.eligibility.entity.EligibilitySession;
import org.netra.features.eligibility.entity.ResultType;
import org.netra.features.eligibility.repository.EligibilitySessionRepository;
import org.netra.features.matching.dto.DonorMatchDto;
import org.netra.features.matching.dto.DonorMatchResponse;
import org.netra.features.matching.dto.MatchQuality;
import org.netra.features.matching.repository.DonorCandidateProjection;
import org.netra.features.matching.repository.DonorMatchingRepository;
import org.netra.features.matching.rules.BloodCompatibilityMatrix;
import org.netra.features.matching.rules.CompatibilityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core matching service that identifies, filters, and ranks compatible donor candidates
 * for an open Blood Request.
 *
 * Invariant Guarantees:
 * - Read / decision-support only in V1: does NOT mutate BloodRequest, DonorProfile, Inventory, or Eligibility records.
 * - Does not claim medical clearance; results represent preliminary availability and ABO/Rh compatibility candidates.
 * - Strict Result Privacy: zero donor contact information (phone, email, home address, GPS coordinates) exposed.
 * - Centralized medical compatibility rule evaluation via BloodCompatibilityMatrix.
 */
@Service
public class DonorMatchingService {

    private static final Logger log = LoggerFactory.getLogger(DonorMatchingService.class);

    private final BloodRequestRepository bloodRequestRepository;
    private final BloodRequestAuthorizationService authorizationService;
    private final DonorMatchingRepository donorMatchingRepository;
    private final BloodCompatibilityMatrix compatibilityMatrix;
    private final EligibilitySessionRepository eligibilitySessionRepository;
    private final RateLimitingService rateLimitingService;
    private final AuditService auditService;
    private final Clock clock;

    private final double defaultRadiusKm;
    private final double maxRadiusKm;
    private final int defaultLimit;
    private final int maxLimit;
    private final int minDonationIntervalDays;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.netra.core.observability.NetraMetrics netraMetrics;

    public void setNetraMetrics(org.netra.core.observability.NetraMetrics netraMetrics) {
        this.netraMetrics = netraMetrics;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public DonorMatchingService(
            BloodRequestRepository bloodRequestRepository,
            BloodRequestAuthorizationService authorizationService,
            DonorMatchingRepository donorMatchingRepository,
            BloodCompatibilityMatrix compatibilityMatrix,
            EligibilitySessionRepository eligibilitySessionRepository,
            RateLimitingService rateLimitingService,
            AuditService auditService,
            java.util.Optional<Clock> clock,
            @Value("${netra.matching.default-radius-km:25.0}") double defaultRadiusKm,
            @Value("${netra.matching.max-radius-km:100.0}") double maxRadiusKm,
            @Value("${netra.matching.default-limit:20}") int defaultLimit,
            @Value("${netra.matching.max-limit:50}") int maxLimit,
            @Value("${netra.matching.min-donation-interval-days:90}") int minDonationIntervalDays) {
        this(bloodRequestRepository, authorizationService, donorMatchingRepository, compatibilityMatrix,
                eligibilitySessionRepository, rateLimitingService, auditService, clock.orElse(Clock.systemUTC()),
                defaultRadiusKm, maxRadiusKm, defaultLimit, maxLimit, minDonationIntervalDays);
    }

    public DonorMatchingService(
            BloodRequestRepository bloodRequestRepository,
            BloodRequestAuthorizationService authorizationService,
            DonorMatchingRepository donorMatchingRepository,
            BloodCompatibilityMatrix compatibilityMatrix,
            EligibilitySessionRepository eligibilitySessionRepository,
            RateLimitingService rateLimitingService,
            AuditService auditService,
            Clock clock,
            double defaultRadiusKm,
            double maxRadiusKm,
            int defaultLimit,
            int maxLimit,
            int minDonationIntervalDays) {
        this.bloodRequestRepository = bloodRequestRepository;
        this.authorizationService = authorizationService;
        this.donorMatchingRepository = donorMatchingRepository;
        this.compatibilityMatrix = compatibilityMatrix;
        this.eligibilitySessionRepository = eligibilitySessionRepository;
        this.rateLimitingService = rateLimitingService;
        this.auditService = auditService;
        this.clock = clock != null ? clock : Clock.systemUTC();
        this.defaultRadiusKm = defaultRadiusKm;
        this.maxRadiusKm = maxRadiusKm;
        this.defaultLimit = defaultLimit;
        this.maxLimit = maxLimit;
        this.minDonationIntervalDays = minDonationIntervalDays;
    }

    public DonorMatchingService(
            BloodRequestRepository bloodRequestRepository,
            BloodRequestAuthorizationService authorizationService,
            DonorMatchingRepository donorMatchingRepository,
            BloodCompatibilityMatrix compatibilityMatrix,
            EligibilitySessionRepository eligibilitySessionRepository,
            RateLimitingService rateLimitingService,
            AuditService auditService,
            double defaultRadiusKm,
            double maxRadiusKm,
            int defaultLimit,
            int maxLimit,
            int minDonationIntervalDays) {
        this(bloodRequestRepository, authorizationService, donorMatchingRepository, compatibilityMatrix,
                eligibilitySessionRepository, rateLimitingService, auditService, Clock.systemUTC(),
                defaultRadiusKm, maxRadiusKm, defaultLimit, maxLimit, minDonationIntervalDays);
    }

    @Transactional(readOnly = true)
    public DonorMatchResponse findMatches(
            UUID requestId,
            UUID currentUserId,
            Double requestedRadiusKm,
            Integer requestedLimit,
            String clientIp,
            String userAgent) {

        long startMs = System.currentTimeMillis();

        // 1. Authenticate user & enforce anti-abuse rate limiting
        authorizationService.verifyActiveUser(currentUserId);
        rateLimitingService.checkMatchingRateLimit(currentUserId.toString());

        // 2. Load Blood Request
        BloodRequest request = bloodRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found with id: " + requestId));

        // 3. Verify owner / admin authorization
        authorizationService.verifyCanManageRequest(currentUserId, request);

        // 4. Validate request lifecycle status (terminal requests cannot be matched)
        if (request.getStatus() != BloodRequestStatus.OPEN) {
            throw new ValidationException("Cannot match donors for blood request with terminal status: " + request.getStatus());
        }

        // 5. Validate request deadline (overdue requests cannot be matched)
        Instant now = clock.instant();
        if (request.getRequiredBy() == null || !request.getRequiredBy().isAfter(now)) {
            throw new ValidationException("Cannot match donors for an overdue blood request.");
        }

        // 6. Validate request coordinates
        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new ValidationException("Blood request does not have valid coordinates for distance matching.");
        }

        // 7. Validate input parameters
        double radiusKm = requestedRadiusKm != null ? requestedRadiusKm : defaultRadiusKm;
        if (radiusKm <= 0.0 || radiusKm > maxRadiusKm) {
            throw new ValidationException("Search radius must be between 0.1 and " + maxRadiusKm + " km.");
        }

        int limit = requestedLimit != null ? requestedLimit : defaultLimit;
        if (limit <= 0 || limit > maxLimit) {
            throw new ValidationException("Result limit must be between 1 and " + maxLimit + ".");
        }

        // 8. Get compatible blood groups from authoritative matrix
        Set<BloodGroup> compatibleGroups = compatibilityMatrix.getCompatibleDonorGroups(request.getBloodGroup());
        if (compatibleGroups.isEmpty()) {
            return new DonorMatchResponse(requestId, request.getBloodGroup(), request.getUrgency(), radiusKm, 0, Collections.emptyList());
        }

        // 9. Compute Stage 1 Geographic Bounding Box and Date Threshold
        double latDelta = radiusKm / 111.0;
        double minLat = Math.max(-90.0, request.getLatitude() - latDelta);
        double maxLat = Math.min(90.0, request.getLatitude() + latDelta);

        double cosLat = Math.cos(Math.toRadians(request.getLatitude()));
        double minLng;
        double maxLng;
        if (cosLat < 0.0001) {
            minLng = -180.0;
            maxLng = 180.0;
        } else {
            double lngDelta = radiusKm / (111.0 * cosLat);
            if (request.getLongitude() - lngDelta < -180.0 || request.getLongitude() + lngDelta > 180.0) {
                minLng = -180.0;
                maxLng = 180.0;
            } else {
                minLng = request.getLongitude() - lngDelta;
                maxLng = request.getLongitude() + lngDelta;
            }
        }

        LocalDate today = LocalDate.ofInstant(now, clock.getZone());
        LocalDate maxLastDonationDate = today.minusDays(minDonationIntervalDays);

        // Query candidate donors with server-side hard filters pushed to database
        List<DonorCandidateProjection> candidates = donorMatchingRepository.findCandidateDonors(
                compatibleGroups,
                request.getRequesterUserId(),
                minLat,
                maxLat,
                minLng,
                maxLng,
                maxLastDonationDate
        );

        // Batch load latest eligibility sessions for all candidates to avoid N+1 queries
        List<UUID> candidateUserIds = candidates.stream()
                .map(DonorCandidateProjection::getUserId)
                .collect(Collectors.toList());

        Map<UUID, EligibilitySession> latestSessionMap = new HashMap<>();
        if (!candidateUserIds.isEmpty()) {
            List<EligibilitySession> sessions = eligibilitySessionRepository.findByUserIdInOrderByStartedAtDesc(candidateUserIds);
            for (EligibilitySession session : sessions) {
                latestSessionMap.putIfAbsent(session.getUserId(), session);
            }
        }

        List<CandidateMatch> candidateMatches = new ArrayList<>();

        for (DonorCandidateProjection candidate : candidates) {
            // Filter 1: Donation history interval check (defensive, also filtered in SQL)
            if (candidate.getLastDonationDate() != null) {
                long daysSinceLast = ChronoUnit.DAYS.between(candidate.getLastDonationDate(), today);
                if (daysSinceLast < minDonationIntervalDays) {
                    continue; // Exclude due to mandatory recovery interval
                }
            }

            // Filter 2: Preliminary eligibility self-screening check (if available)
            // Eligibility Semantics:
            // - Eligibility sessions represent preliminary self-screening, NOT permanent medical clearance.
            // - No permanent donor.isEligible flag exists.
            // - If the donor's latest session indicates an active TEMPORARY_DEFERRAL (with estimatedEligibleDate > today)
            //   or MEDICAL_REVIEW_REQUIRED, the donor is excluded from the match candidate pool.
            // - Donors without screening sessions, or whose deferral has elapsed, or who completed self-screening with LIKELY_ELIGIBLE
            //   are included as preliminary candidates, but still require formal clinical evaluation prior to donation.
            // - An expired session or lack of a session does not grant permanent clearance; matching remains decision-support only.
            EligibilitySession latest = latestSessionMap.get(candidate.getUserId());
            if (latest != null) {
                if (latest.getResult() == ResultType.MEDICAL_REVIEW_REQUIRED) {
                    continue;
                }
                if (latest.getResult() == ResultType.TEMPORARY_DEFERRAL) {
                    if (latest.getEstimatedEligibleDate() != null && latest.getEstimatedEligibleDate().isAfter(today)) {
                        continue;
                    }
                }
            }

            // Filter 3: Stage 2 Proximity & Boundary calculation (Haversine distance within exact radius)
            if (candidate.getLatitude() == null || candidate.getLongitude() == null) {
                continue;
            }

            double rawDistance = BloodBankService.calculateHaversineDistanceKm(
                    request.getLatitude(), request.getLongitude(),
                    candidate.getLatitude(), candidate.getLongitude()
            );

            if (rawDistance > radiusKm) {
                continue; // Outside requested search radius
            }

            double distanceKm = Math.round(rawDistance * 10.0) / 10.0;

            // Determine Compatibility Type & Match Quality (all candidates are VERIFIED)
            CompatibilityType compatibilityType = compatibilityMatrix.getCompatibilityType(
                    candidate.getBloodGroup(), request.getBloodGroup()
            );

            MatchQuality quality = (compatibilityType == CompatibilityType.EXACT && distanceKm <= 25.0)
                    ? MatchQuality.EXCELLENT
                    : MatchQuality.GOOD;

            // Mask name safely (e.g. "John D.")
            String maskedName = maskDisplayName(candidate.getFullName());
            UUID candidateReference = candidate.getDonorProfileId();

            DonorMatchDto dto = new DonorMatchDto(
                    candidateReference,
                    maskedName,
                    candidate.getBloodGroup(),
                    candidate.getBloodGroupVerificationStatus(),
                    candidate.getAvailabilityStatus(),
                    distanceKm,
                    compatibilityType,
                    quality
            );

            candidateMatches.add(new CandidateMatch(candidate, dto));
        }

        // 9. Deterministic Ranking:
        // 1. EXACT compatibility before COMPATIBLE
        // 2. distanceKm ascending (closest donors first)
        // 3. donorProfileId ascending as an internal deterministic tie-breaker
        candidateMatches.sort(Comparator
                // Exact compatibility ranks before compatible
                .comparingInt((CandidateMatch cm) -> cm.dto.getCompatibilityType() == CompatibilityType.EXACT ? 0 : 1)
                // Proximity: closest donors first
                .thenComparingDouble(cm -> cm.dto.getDistanceKm())
                // Deterministic tie-breaker using internal donor profile ID (never exposed to client)
                .thenComparing(cm -> cm.candidate.getDonorProfileId().toString())
        );

        // 10. Truncate to limit
        List<DonorMatchDto> limitedMatches = candidateMatches.stream()
                .limit(limit)
                .map(cm -> cm.dto)
                .collect(Collectors.toList());

        // 11. Security Audit Logging (Never log private donor data)
        auditService.logAuthEvent(
                "DONOR_MATCHING_EXECUTED",
                currentUserId,
                clientIp,
                userAgent,
                String.format("{\"requestId\":\"%s\",\"candidateCount\":%d,\"radiusKm\":%.1f}",
                        requestId, limitedMatches.size(), radiusKm)
        );

        long durationMs = System.currentTimeMillis() - startMs;
        if (netraMetrics != null) {
            netraMetrics.recordMatchingDuration(durationMs);
        }
        org.netra.core.observability.StructuredLogger.logOperation(
                "DONOR_MATCHING_SEARCH", currentUserId, null, "BloodRequest", requestId, "SEARCH", durationMs, "SUCCESS");

        return new DonorMatchResponse(
                requestId,
                request.getBloodGroup(),
                request.getUrgency(),
                radiusKm,
                limitedMatches.size(),
                limitedMatches
        );
    }

    /**
     * Sanitizes full name to protect donor privacy before explicit consent.
     * E.g. "John Doe" -> "John D."
     */
    public static String maskDisplayName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "Anonymous Donor";
        }
        String trimmed = fullName.trim();
        String[] parts = trimmed.split("\\s+");
        if (parts.length == 1) {
            return parts[0];
        }
        String first = parts[0];
        String lastInitial = parts[parts.length - 1].substring(0, 1).toUpperCase();
        return first + " " + lastInitial + ".";
    }

    private static class CandidateMatch {
        final DonorCandidateProjection candidate;
        final DonorMatchDto dto;

        CandidateMatch(DonorCandidateProjection candidate, DonorMatchDto dto) {
            this.candidate = candidate;
            this.dto = dto;
        }
    }
}
