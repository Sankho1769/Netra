package org.netra.features.matching.service;

import org.netra.core.audit.AuditService;
import org.netra.core.exception.DuplicateResourceException;
import org.netra.core.exception.ResourceNotFoundException;
import org.netra.core.exception.UnauthorizedSessionAccessException;
import org.netra.core.exception.ValidationException;
import org.netra.features.bloodbank.service.BloodBankService;
import org.netra.features.bloodrequest.entity.BloodRequest;
import org.netra.features.bloodrequest.entity.BloodRequestStatus;
import org.netra.features.bloodrequest.repository.BloodRequestRepository;
import org.netra.features.bloodrequest.service.BloodRequestAuthorizationService;
import org.netra.features.donor.entity.*;
import org.netra.features.donor.repository.DonorProfileRepository;
import org.netra.features.eligibility.entity.EligibilitySession;
import org.netra.features.eligibility.entity.ResultType;
import org.netra.features.eligibility.entity.SessionStatus;
import org.netra.features.eligibility.repository.EligibilitySessionRepository;
import org.netra.features.eligibility.service.DonationEligibilityPolicy;
import org.netra.features.matching.dto.CreateDonorMatchRequest;
import org.netra.features.matching.dto.DonorMatchDetailDto;
import org.netra.features.matching.dto.RequesterDonorMatchDto;
import org.netra.features.matching.entity.DonorMatch;
import org.netra.features.matching.entity.MatchStatus;
import org.netra.features.matching.repository.DonorMatchRepository;
import org.netra.features.matching.rules.BloodCompatibilityMatrix;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserStatus;
import org.netra.features.user.repository.UserRepository;
import org.netra.features.notification.event.MatchAcceptedEvent;
import org.netra.features.notification.event.MatchCreatedEvent;
import org.netra.features.notification.event.MatchDeclinedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service managing persistent donor match workflows and donor responses (accept/decline).
 *
 * Core Principles:
 * - Server is the sole authority; client claims are revalidated against current DB state.
 * - Invariants strictly preserved: responses never alter inventory, request status, or donation dates.
 * - Concurrency protected via atomic compare-and-set queries and database uniqueness constraints.
 */
@Service
public class DonorResponseService {

    private static final Logger log = LoggerFactory.getLogger(DonorResponseService.class);

    private final DonorMatchRepository donorMatchRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final BloodRequestAuthorizationService authorizationService;
    private final DonorProfileRepository donorProfileRepository;
    private final UserRepository userRepository;
    private final EligibilitySessionRepository eligibilitySessionRepository;
    private final BloodCompatibilityMatrix compatibilityMatrix;
    private final AuditService auditService;
    private final DonationEligibilityPolicy donationEligibilityPolicy;
    private final Clock clock;
    private final Duration matchResponseTtl;
    private final double maxRadiusKm;
    private final int minDonationIntervalDays;
    private final int maxActiveMatchesPerRequest;
    private final ApplicationEventPublisher eventPublisher;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.netra.core.observability.NetraMetrics netraMetrics;

    public void setNetraMetrics(org.netra.core.observability.NetraMetrics netraMetrics) {
        this.netraMetrics = netraMetrics;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public DonorResponseService(
            DonorMatchRepository donorMatchRepository,
            BloodRequestRepository bloodRequestRepository,
            BloodRequestAuthorizationService authorizationService,
            DonorProfileRepository donorProfileRepository,
            UserRepository userRepository,
            EligibilitySessionRepository eligibilitySessionRepository,
            BloodCompatibilityMatrix compatibilityMatrix,
            AuditService auditService,
            DonationEligibilityPolicy donationEligibilityPolicy,
            Clock clock,
            @Value("${netra.donor-matching.match-response-ttl:24h}") Duration matchResponseTtl,
            @Value("${netra.matching.max-radius-km:100.0}") double maxRadiusKm,
            @Value("${netra.matching.min-donation-interval-days:90}") int minDonationIntervalDays,
            @Value("${netra.donor-matching.max-active-matches-per-request:10}") int maxActiveMatchesPerRequest,
            org.springframework.beans.factory.ObjectProvider<ApplicationEventPublisher> eventPublisherProvider) {
        this(donorMatchRepository, bloodRequestRepository, authorizationService, donorProfileRepository,
                userRepository, eligibilitySessionRepository, compatibilityMatrix, auditService,
                donationEligibilityPolicy, clock, matchResponseTtl, maxRadiusKm, minDonationIntervalDays,
                maxActiveMatchesPerRequest,
                eventPublisherProvider != null ? eventPublisherProvider.getIfAvailable() : null);
    }

    public DonorResponseService(
            DonorMatchRepository donorMatchRepository,
            BloodRequestRepository bloodRequestRepository,
            BloodRequestAuthorizationService authorizationService,
            DonorProfileRepository donorProfileRepository,
            UserRepository userRepository,
            EligibilitySessionRepository eligibilitySessionRepository,
            BloodCompatibilityMatrix compatibilityMatrix,
            AuditService auditService,
            DonationEligibilityPolicy donationEligibilityPolicy,
            Clock clock,
            Duration matchResponseTtl,
            double maxRadiusKm,
            int minDonationIntervalDays,
            int maxActiveMatchesPerRequest,
            ApplicationEventPublisher eventPublisher) {
        this.donorMatchRepository = donorMatchRepository;
        this.bloodRequestRepository = bloodRequestRepository;
        this.authorizationService = authorizationService;
        this.donorProfileRepository = donorProfileRepository;
        this.userRepository = userRepository;
        this.eligibilitySessionRepository = eligibilitySessionRepository;
        this.compatibilityMatrix = compatibilityMatrix;
        this.auditService = auditService;
        this.donationEligibilityPolicy = donationEligibilityPolicy != null ? donationEligibilityPolicy : new DonationEligibilityPolicy();
        this.clock = clock != null ? clock : Clock.systemUTC();
        this.matchResponseTtl = matchResponseTtl;
        this.maxRadiusKm = maxRadiusKm;
        this.minDonationIntervalDays = minDonationIntervalDays;
        this.maxActiveMatchesPerRequest = maxActiveMatchesPerRequest;
        this.eventPublisher = eventPublisher;
    }

    public DonorResponseService(
            DonorMatchRepository donorMatchRepository,
            BloodRequestRepository bloodRequestRepository,
            BloodRequestAuthorizationService authorizationService,
            DonorProfileRepository donorProfileRepository,
            UserRepository userRepository,
            EligibilitySessionRepository eligibilitySessionRepository,
            BloodCompatibilityMatrix compatibilityMatrix,
            AuditService auditService,
            Clock clock,
            Duration matchResponseTtl,
            double maxRadiusKm,
            int minDonationIntervalDays,
            int maxActiveMatchesPerRequest,
            ApplicationEventPublisher eventPublisher) {
        this(donorMatchRepository, bloodRequestRepository, authorizationService, donorProfileRepository,
                userRepository, eligibilitySessionRepository, compatibilityMatrix, auditService,
                new DonationEligibilityPolicy(), clock, matchResponseTtl, maxRadiusKm,
                minDonationIntervalDays, maxActiveMatchesPerRequest, eventPublisher);
    }

    public DonorResponseService(
            DonorMatchRepository donorMatchRepository,
            BloodRequestRepository bloodRequestRepository,
            BloodRequestAuthorizationService authorizationService,
            DonorProfileRepository donorProfileRepository,
            UserRepository userRepository,
            EligibilitySessionRepository eligibilitySessionRepository,
            BloodCompatibilityMatrix compatibilityMatrix,
            AuditService auditService,
            Clock clock,
            Duration matchResponseTtl,
            double maxRadiusKm,
            int minDonationIntervalDays) {
        this(donorMatchRepository, bloodRequestRepository, authorizationService, donorProfileRepository,
                userRepository, eligibilitySessionRepository, compatibilityMatrix, auditService,
                clock, matchResponseTtl, maxRadiusKm, minDonationIntervalDays, 10, null);
    }

    public DonorResponseService(
            DonorMatchRepository donorMatchRepository,
            BloodRequestRepository bloodRequestRepository,
            BloodRequestAuthorizationService authorizationService,
            DonorProfileRepository donorProfileRepository,
            UserRepository userRepository,
            EligibilitySessionRepository eligibilitySessionRepository,
            BloodCompatibilityMatrix compatibilityMatrix,
            AuditService auditService,
            Clock clock,
            Duration matchResponseTtl,
            double maxRadiusKm,
            int minDonationIntervalDays,
            int maxActiveMatchesPerRequest) {
        this(donorMatchRepository, bloodRequestRepository, authorizationService, donorProfileRepository,
                userRepository, eligibilitySessionRepository, compatibilityMatrix, auditService,
                clock, matchResponseTtl, maxRadiusKm, minDonationIntervalDays, maxActiveMatchesPerRequest, null);
    }

    /**
     * Explicit, authorized creation of a persistent donor match.
     * Revalidates request status and candidate donor eligibility entirely server-side.
     */
    @Transactional
    public RequesterDonorMatchDto createMatch(
            UUID requestId,
            CreateDonorMatchRequest matchRequest,
            UUID currentUserId,
            String clientIp,
            String userAgent) {

        authorizationService.verifyActiveUser(currentUserId);

        UUID candidateRef = matchRequest.getCandidateReference();
        if (candidateRef == null) {
            throw new ValidationException("candidateReference must be provided.");
        }

        Instant now = clock.instant();

        // 1. Authoritative Blood Request verification with pessimistic write lock for concurrency safety
        BloodRequest bloodRequest = bloodRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found with id: " + requestId));

        authorizationService.verifyCanManageRequest(currentUserId, bloodRequest);

        if (bloodRequest.getStatus() != BloodRequestStatus.OPEN) {
            throw new ValidationException("Cannot create match for blood request with status: " + bloodRequest.getStatus());
        }

        if (bloodRequest.getRequiredBy() == null || !bloodRequest.getRequiredBy().isAfter(now)) {
            throw new ValidationException("Cannot create match for overdue blood request.");
        }

        // Active pending matches abuse limit check per BloodRequest
        int activeMatches = donorMatchRepository.countByBloodRequestIdAndResponseStatus(requestId, MatchStatus.MATCHED);
        if (activeMatches >= maxActiveMatchesPerRequest) {
            throw new ValidationException("Maximum number of active pending matches (" + maxActiveMatchesPerRequest +
                    ") reached for this blood request. Wait for donors to respond or expire.");
        }

        // 2. Authoritative Server-side Donor Revalidation
        // candidateReference is currently the DonorProfile UUID: an authorized internal donor-selection reference. Not a secret.
        // It is accepted only from authorized request owners and admins, and requires complete server-side revalidation.
        DonorProfile donorProfile = donorProfileRepository.findById(candidateRef)
                .orElseThrow(() -> new ValidationException("Donor candidate not found: " + candidateRef));

        UUID donorUserId = donorProfile.getUserId();

        User donorUser = userRepository.findById(donorUserId)
                .orElseThrow(() -> new ValidationException("Donor user not found: " + donorUserId));

        if (donorUser.getStatus() != UserStatus.ACTIVE) {
            throw new ValidationException("Donor user is not active.");
        }

        if (donorProfile.getDonorStatus() != DonorStatus.ACTIVE) {
            throw new ValidationException("Donor profile is not active.");
        }

        if (donorProfile.getAvailabilityStatus() != DonorAvailabilityStatus.AVAILABLE) {
            throw new ValidationException("Donor is currently not available for donation.");
        }

        // V1 Policy: Verified blood-group only
        if (donorProfile.getBloodGroupVerificationStatus() != BloodGroupVerificationStatus.VERIFIED) {
            throw new ValidationException("Only verified blood-group donors are eligible for matching.");
        }

        // ABO/Rh Compatibility Recheck
        if (!compatibilityMatrix.isCompatible(donorProfile.getBloodGroup(), bloodRequest.getBloodGroup())) {
            throw new ValidationException("Donor blood group " + donorProfile.getBloodGroup() +
                    " is incompatible with requested blood group " + bloodRequest.getBloodGroup());
        }

        // Coordinate & Distance Recheck
        if (donorProfile.getLatitude() == null || donorProfile.getLongitude() == null) {
            throw new ValidationException("Donor does not have registered coordinates.");
        }
        if (bloodRequest.getLatitude() == null || bloodRequest.getLongitude() == null) {
            throw new ValidationException("Blood request does not have registered coordinates.");
        }

        // Evaluate RAW distance before any rounding occurs
        double rawDistanceKm = BloodBankService.calculateHaversineDistanceKm(
                bloodRequest.getLatitude(), bloodRequest.getLongitude(),
                donorProfile.getLatitude(), donorProfile.getLongitude()
        );

        if (rawDistanceKm > maxRadiusKm) {
            throw new ValidationException("Donor distance (" + Math.round(rawDistanceKm * 10.0) / 10.0 +
                    " km) exceeds maximum matching radius (" + maxRadiusKm + " km).");
        }

        double distanceKm = Math.round(rawDistanceKm * 10.0) / 10.0;

        // Mandatory Donation Interval using authoritative DonationEligibilityPolicy
        LocalDate today = LocalDate.ofInstant(now, clock.getZone());
        if (donorProfile.getLastDonationDate() != null) {
            if (!donationEligibilityPolicy.isIntervalEligible(donorProfile.getBiologicalSex(), donorProfile.getLastDonationDate(), today)) {
                int requiredDays = donationEligibilityPolicy.getRequiredIntervalDays(donorProfile.getBiologicalSex());
                throw new ValidationException("Donor has donated too recently. Minimum interval is " + requiredDays + " days.");
            }
        }

        // Eligibility Screening Session Recheck (COMPLETED sessions only)
        List<EligibilitySession> sessions = eligibilitySessionRepository
                .findByUserIdInAndStatusOrderByCompletedAtDesc(List.of(donorUserId), SessionStatus.COMPLETED);
        if (!sessions.isEmpty()) {
            EligibilitySession latest = sessions.get(0);
            if (latest.getResult() == ResultType.MEDICAL_REVIEW_REQUIRED) {
                throw new ValidationException("Donor requires medical review prior to donation.");
            }
            if (latest.getResult() == ResultType.TEMPORARY_DEFERRAL) {
                if (latest.getEstimatedEligibleDate() == null || latest.getEstimatedEligibleDate().isAfter(today)) {
                    throw new ValidationException("Donor has an active temporary deferral" +
                            (latest.getEstimatedEligibleDate() != null ? " until " + latest.getEstimatedEligibleDate() : "."));
                }
            }
        }

        // 3. Duplicate Match Check
        if (donorMatchRepository.existsByBloodRequestIdAndDonorUserId(requestId, donorUserId)) {
            throw new DuplicateResourceException("A match record already exists for this blood request and donor.");
        }

        // 4. Calculate Expiration: min(now + TTL, request.requiredBy)
        Instant calculatedExpiry = now.plus(matchResponseTtl);
        Instant finalExpiry = calculatedExpiry.isBefore(bloodRequest.getRequiredBy()) ? calculatedExpiry : bloodRequest.getRequiredBy();

        // 5. Persist Match
        DonorMatch match = new DonorMatch(requestId, donorUserId, now, finalExpiry);

        DonorMatch savedMatch;
        try {
            savedMatch = donorMatchRepository.saveAndFlush(match);
        } catch (DataIntegrityViolationException ex) {
            if (isUniqueMatchConstraintViolation(ex)) {
                throw new DuplicateResourceException("A match record already exists for this blood request and donor.");
            }
            throw ex;
        }

        // 6. Security Audit Event (Safe metadata only)
        auditService.logAuthEvent(
                "DONOR_MATCH_CREATED",
                currentUserId,
                clientIp,
                userAgent,
                String.format("{\"matchId\":\"%s\",\"bloodRequestId\":\"%s\"}",
                        savedMatch.getId(), requestId)
        );

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new MatchCreatedEvent(
                    savedMatch.getId(),
                    requestId,
                    donorUserId,
                    bloodRequest.getRequesterUserId()
            ));
        }

        if (netraMetrics != null) {
            netraMetrics.incrementDonorMatchesCreated(1);
        }
        org.netra.core.observability.StructuredLogger.logOperation(
                "DONOR_MATCH_CREATED", currentUserId, null, "DonorMatch", savedMatch.getId(), "CREATE", null, "SUCCESS");

        String maskedName = DonorMatchingService.maskDisplayName(donorUser.getFullName());
        return new RequesterDonorMatchDto(
                savedMatch.getId(),
                requestId,
                maskedName,
                donorProfile.getBloodGroup(),
                donorProfile.getBloodGroupVerificationStatus(),
                donorProfile.getAvailabilityStatus(),
                distanceKm,
                savedMatch.getResponseStatus(),
                savedMatch.getCreatedAt(),
                savedMatch.getUpdatedAt(),
                savedMatch.getRespondedAt(),
                savedMatch.getExpiresAt()
        );
    }

    /**
     * Retrieves all persistent matches assigned to the authenticated donor.
     */
    @Transactional(readOnly = true)
    public List<DonorMatchDetailDto> getMatchesForDonor(UUID currentUserId) {
        authorizationService.verifyActiveUser(currentUserId);
        List<DonorMatch> matches = donorMatchRepository.findByDonorUserIdOrderByCreatedAtDesc(currentUserId);
        if (matches.isEmpty()) {
            return Collections.emptyList();
        }

        Set<UUID> requestIds = matches.stream().map(DonorMatch::getBloodRequestId).collect(Collectors.toSet());
        Map<UUID, BloodRequest> requestMap = bloodRequestRepository.findAllById(requestIds).stream()
                .collect(Collectors.toMap(BloodRequest::getId, r -> r));

        Optional<DonorProfile> donorProfileOpt = donorProfileRepository.findByUserId(currentUserId);
        Double donorLat = donorProfileOpt.map(DonorProfile::getLatitude).orElse(null);
        Double donorLng = donorProfileOpt.map(DonorProfile::getLongitude).orElse(null);

        return matches.stream().map(m -> {
            BloodRequest req = requestMap.get(m.getBloodRequestId());
            Double distanceKm = null;
            if (req != null && req.getLatitude() != null && req.getLongitude() != null && donorLat != null && donorLng != null) {
                distanceKm = Math.round(BloodBankService.calculateHaversineDistanceKm(
                        req.getLatitude(), req.getLongitude(), donorLat, donorLng
                ) * 10.0) / 10.0;
            }
            return mapToDonorDetailDto(m, req, distanceKm);
        }).collect(Collectors.toList());
    }

    /**
     * Retrieves a single persistent match for the authenticated donor.
     */
    @Transactional(readOnly = true)
    public DonorMatchDetailDto getMatchForDonor(UUID matchId, UUID currentUserId) {
        authorizationService.verifyActiveUser(currentUserId);

        DonorMatch match = donorMatchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Donor match not found with id: " + matchId));

        if (!match.getDonorUserId().equals(currentUserId)) {
            throw new UnauthorizedSessionAccessException("Access denied to donor match.");
        }

        BloodRequest req = bloodRequestRepository.findById(match.getBloodRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found with id: " + match.getBloodRequestId()));

        Optional<DonorProfile> donorProfileOpt = donorProfileRepository.findByUserId(currentUserId);
        Double distanceKm = null;
        if (req.getLatitude() != null && req.getLongitude() != null && donorProfileOpt.isPresent()) {
            DonorProfile dp = donorProfileOpt.get();
            if (dp.getLatitude() != null && dp.getLongitude() != null) {
                distanceKm = Math.round(BloodBankService.calculateHaversineDistanceKm(
                        req.getLatitude(), req.getLongitude(), dp.getLatitude(), dp.getLongitude()
                ) * 10.0) / 10.0;
            }
        }

        return mapToDonorDetailDto(match, req, distanceKm);
    }

    /**
     * Records a donor's acceptance of a match.
     *
     * Invariants:
     * - Does not alter BloodRequest status
     * - Does not alter BloodInventory
     * - Does not alter DonorProfile.lastDonationDate
     * - Does not create a verified donation or medical clearance
     */
    @Transactional
    public DonorMatchDetailDto acceptMatch(UUID matchId, UUID currentUserId, String clientIp, String userAgent) {
        authorizationService.verifyActiveUser(currentUserId);

        DonorMatch match = donorMatchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Donor match not found with id: " + matchId));

        if (!match.getDonorUserId().equals(currentUserId)) {
            throw new UnauthorizedSessionAccessException("Access denied to donor match.");
        }

        BloodRequest req = bloodRequestRepository.findById(match.getBloodRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found with id: " + match.getBloodRequestId()));

        if (req.getStatus() != BloodRequestStatus.OPEN) {
            throw new ValidationException("Cannot accept match for blood request with status: " + req.getStatus());
        }

        Instant now = clock.instant();

        // Re-validate donor eligibility prior to CAS acceptance
        Optional<DonorProfile> donorProfileOpt = donorProfileRepository.findByUserId(currentUserId);
        if (donorProfileOpt.isPresent()) {
            DonorProfile donorProfile = donorProfileOpt.get();
            LocalDate today = LocalDate.ofInstant(now, clock.getZone());
            if (donorProfile.getLastDonationDate() != null &&
                    !donationEligibilityPolicy.isIntervalEligible(donorProfile.getBiologicalSex(), donorProfile.getLastDonationDate(), today)) {
                int requiredDays = donationEligibilityPolicy.getRequiredIntervalDays(donorProfile.getBiologicalSex());
                throw new ValidationException("Donor has donated too recently. Minimum interval is " + requiredDays + " days.");
            }
            List<EligibilitySession> completedSessions = eligibilitySessionRepository
                    .findByUserIdInAndStatusOrderByCompletedAtDesc(List.of(currentUserId), SessionStatus.COMPLETED);
            if (!completedSessions.isEmpty()) {
                EligibilitySession latest = completedSessions.get(0);
                if (latest.getResult() == ResultType.MEDICAL_REVIEW_REQUIRED) {
                    throw new ValidationException("Donor requires medical review prior to donation.");
                }
                if (latest.getResult() == ResultType.TEMPORARY_DEFERRAL) {
                    if (latest.getEstimatedEligibleDate() == null || latest.getEstimatedEligibleDate().isAfter(today)) {
                        throw new ValidationException("Donor has an active temporary deferral" +
                                (latest.getEstimatedEligibleDate() != null ? " until " + latest.getEstimatedEligibleDate() : "."));
                    }
                }
            }
        }

        // Atomic CAS transition: MATCHED -> ACCEPTED where expiresAt > now
        int updated = donorMatchRepository.atomicTransitionStatus(
                matchId, currentUserId, MatchStatus.MATCHED, MatchStatus.ACCEPTED, now
        );

        if (updated == 0) {
            DonorMatch fresh = donorMatchRepository.findById(matchId).orElse(match);
            if (fresh.getResponseStatus() == MatchStatus.ACCEPTED) {
                throw new ValidationException("Match has already been accepted.");
            }
            if (fresh.getResponseStatus() != MatchStatus.MATCHED) {
                throw new ValidationException("Cannot accept match with status: " + fresh.getResponseStatus());
            }
            if (fresh.isExpired(now)) {
                throw new ValidationException("Cannot accept an expired match.");
            }
            throw new ValidationException("Unable to accept match due to state conflict.");
        }

        auditService.logAuthEvent(
                "DONOR_MATCH_ACCEPTED",
                currentUserId,
                clientIp,
                userAgent,
                String.format("{\"matchId\":\"%s\",\"bloodRequestId\":\"%s\"}", matchId, req.getId())
        );

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new MatchAcceptedEvent(
                    matchId,
                    req.getId(),
                    currentUserId,
                    req.getRequesterUserId()
            ));
        }

        if (netraMetrics != null) {
            netraMetrics.incrementDonorMatchResponse("ACCEPTED");
        }
        org.netra.core.observability.StructuredLogger.logOperation(
                "DONOR_MATCH_ACCEPTED", currentUserId, null, "DonorMatch", matchId, "ACCEPT", null, "SUCCESS");

        DonorMatch updatedMatch = donorMatchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Donor match not found with id: " + matchId));

        return mapToDonorDetailDto(updatedMatch, req, null);
    }

    /**
     * Records a donor's decline of a match.
     */
    @Transactional
    public DonorMatchDetailDto declineMatch(UUID matchId, UUID currentUserId, String clientIp, String userAgent) {
        authorizationService.verifyActiveUser(currentUserId);

        DonorMatch match = donorMatchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Donor match not found with id: " + matchId));

        if (!match.getDonorUserId().equals(currentUserId)) {
            throw new UnauthorizedSessionAccessException("Access denied to donor match.");
        }

        BloodRequest req = bloodRequestRepository.findById(match.getBloodRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found with id: " + match.getBloodRequestId()));

        if (req.getStatus() != BloodRequestStatus.OPEN) {
            throw new ValidationException("Cannot decline match for blood request with status: " + req.getStatus());
        }

        Instant now = clock.instant();

        if (req.getRequiredBy() != null && !req.getRequiredBy().isAfter(now)) {
            throw new ValidationException("Cannot decline match for overdue blood request.");
        }

        if (match.isExpired(now)) {
            throw new ValidationException("Cannot decline an expired match.");
        }

        if (match.getResponseStatus() != MatchStatus.MATCHED) {
            throw new ValidationException("Cannot decline match with status: " + match.getResponseStatus());
        }

        // Atomic CAS transition: MATCHED -> DECLINED where expiresAt > now
        int updated = donorMatchRepository.atomicTransitionStatus(
                matchId, currentUserId, MatchStatus.MATCHED, MatchStatus.DECLINED, now
        );

        if (updated == 0) {
            DonorMatch fresh = donorMatchRepository.findById(matchId).orElse(match);
            if (fresh.getResponseStatus() != MatchStatus.MATCHED) {
                throw new ValidationException("Cannot decline match with status: " + fresh.getResponseStatus());
            }
            if (fresh.isExpired(now)) {
                throw new ValidationException("Cannot decline an expired match.");
            }
            throw new ValidationException("Unable to decline match due to state conflict.");
        }

        auditService.logAuthEvent(
                "DONOR_MATCH_DECLINED",
                currentUserId,
                clientIp,
                userAgent,
                String.format("{\"matchId\":\"%s\",\"bloodRequestId\":\"%s\"}", matchId, match.getBloodRequestId())
        );

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new MatchDeclinedEvent(
                    matchId,
                    match.getBloodRequestId(),
                    currentUserId,
                    req.getRequesterUserId()
            ));
        }

        if (netraMetrics != null) {
            netraMetrics.incrementDonorMatchResponse("DECLINED");
        }
        org.netra.core.observability.StructuredLogger.logOperation(
                "DONOR_MATCH_DECLINED", currentUserId, null, "DonorMatch", matchId, "DECLINE", null, "SUCCESS");

        DonorMatch updatedMatch = donorMatchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Donor match not found with id: " + matchId));

        return mapToDonorDetailDto(updatedMatch, req, null);
    }

    /**
     * Retrieves all persistent matches for a blood request.
     * Authorized only for request owner or ADMIN.
     */
    @Transactional(readOnly = true)
    public List<RequesterDonorMatchDto> getMatchesForRequest(UUID requestId, UUID currentUserId) {
        authorizationService.verifyActiveUser(currentUserId);

        BloodRequest bloodRequest = bloodRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found with id: " + requestId));

        authorizationService.verifyCanManageRequest(currentUserId, bloodRequest);

        List<DonorMatch> matches = donorMatchRepository.findByBloodRequestIdOrderByCreatedAtDesc(requestId);
        if (matches.isEmpty()) {
            return Collections.emptyList();
        }

        Set<UUID> donorUserIds = matches.stream().map(DonorMatch::getDonorUserId).collect(Collectors.toSet());
        Map<UUID, User> userMap = userRepository.findAllById(donorUserIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<UUID, DonorProfile> profileMap = donorProfileRepository.findAllByUserIdIn(donorUserIds).stream()
                .collect(Collectors.toMap(DonorProfile::getUserId, p -> p));

        return matches.stream().map(m -> {
            User u = userMap.get(m.getDonorUserId());
            DonorProfile dp = profileMap.get(m.getDonorUserId());
            String maskedName = u != null ? DonorMatchingService.maskDisplayName(u.getFullName()) : "Anonymous Donor";
            Double distanceKm = null;
            if (bloodRequest.getLatitude() != null && bloodRequest.getLongitude() != null &&
                    dp != null && dp.getLatitude() != null && dp.getLongitude() != null) {
                distanceKm = Math.round(BloodBankService.calculateHaversineDistanceKm(
                        bloodRequest.getLatitude(), bloodRequest.getLongitude(),
                        dp.getLatitude(), dp.getLongitude()
                ) * 10.0) / 10.0;
            }

            return new RequesterDonorMatchDto(
                    m.getId(),
                    m.getBloodRequestId(),
                    maskedName,
                    dp != null ? dp.getBloodGroup() : null,
                    dp != null ? dp.getBloodGroupVerificationStatus() : null,
                    dp != null ? dp.getAvailabilityStatus() : null,
                    distanceKm,
                    m.getResponseStatus(),
                    m.getCreatedAt(),
                    m.getUpdatedAt(),
                    m.getRespondedAt(),
                    m.getExpiresAt()
            );
        }).collect(Collectors.toList());
    }

    private DonorMatchDetailDto mapToDonorDetailDto(DonorMatch match, BloodRequest req, Double distanceKm) {
        return new DonorMatchDetailDto(
                match.getId(),
                match.getBloodRequestId(),
                req != null ? req.getBloodGroup() : null,
                req != null ? req.getUnitsRequired() : null,
                req != null ? req.getUrgency() : null,
                req != null ? req.getHospitalName() : null,
                req != null ? req.getCity() : null,
                req != null ? req.getState() : null,
                distanceKm,
                req != null ? req.getRequiredBy() : null,
                match.getExpiresAt(),
                match.getResponseStatus(),
                match.getCreatedAt(),
                match.getRespondedAt()
        );
    }

    public boolean isUniqueMatchConstraintViolation(DataIntegrityViolationException ex) {
        // 1. Prefer explicit Hibernate ConstraintViolationException and getConstraintName()
        Throwable current = ex;
        while (current != null) {
            if (current instanceof org.hibernate.exception.ConstraintViolationException cve) {
                String constraintName = cve.getConstraintName();
                if (constraintName != null && !constraintName.isBlank()) {
                    return "uq_donor_matches_request_donor".equalsIgnoreCase(constraintName);
                }
            }
            current = current.getCause();
        }

        // 2. Fallback: message / root-cause string matching only when explicit constraint name is unavailable
        String msg = ex.getMessage();
        if (ex.getRootCause() != null && ex.getRootCause().getMessage() != null) {
            msg = (msg != null ? msg + " " : "") + ex.getRootCause().getMessage();
        }
        if (msg == null) {
            return false;
        }
        String lower = msg.toLowerCase();
        return lower.contains("uq_donor_matches_request_donor") ||
                (lower.contains("unique") && lower.contains("blood_request_id") && lower.contains("donor_user_id"));
    }
}
