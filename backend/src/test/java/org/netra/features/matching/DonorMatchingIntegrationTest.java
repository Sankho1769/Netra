package org.netra.features.matching;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.core.ratelimit.RateLimitingService;
import org.netra.core.security.JwtTokenProvider;
import org.netra.features.bloodrequest.entity.BloodRequest;
import org.netra.features.bloodrequest.entity.BloodRequestStatus;
import org.netra.features.bloodrequest.entity.BloodRequestUrgency;
import org.netra.features.bloodrequest.repository.BloodRequestRepository;
import org.netra.features.donor.entity.*;
import org.netra.features.donor.repository.DonorProfileRepository;
import org.netra.features.eligibility.entity.EligibilitySession;
import org.netra.features.eligibility.entity.ResultType;
import org.netra.features.eligibility.repository.EligibilitySessionRepository;
import org.netra.features.matching.repository.DonorMatchingRepository;
import org.netra.features.matching.rules.CompatibilityType;
import org.netra.features.matching.dto.MatchQuality;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.entity.UserStatus;
import org.netra.features.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DonorMatchingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private DonorProfileRepository donorProfileRepository;

    @Autowired
    private DonorMatchingRepository donorMatchingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EligibilitySessionRepository eligibilitySessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RateLimitingService rateLimitingService;

    private User requester;
    private String requesterToken;

    // Reference base center: Mumbai CST (18.9400, 72.8350)
    private static final double REQ_LAT = 18.9400;
    private static final double REQ_LNG = 72.8350;

    @BeforeEach
    void setUp() {
        eligibilitySessionRepository.deleteAll();
        donorProfileRepository.deleteAll();
        bloodRequestRepository.deleteAll();
        rateLimitingService.reset();

        requester = createTestUser("req.integ", UserRole.ROLE_RECEIVER, UserStatus.ACTIVE);
        requesterToken = jwtTokenProvider.generateAccessToken(requester.getId(), List.of("ROLE_RECEIVER"));
    }

    private User createTestUser(String namePrefix, UserRole role, UserStatus status) {
        User user = new User(
                namePrefix + " FullName",
                namePrefix + "." + UUID.randomUUID() + "@netra.org",
                "+919800000000",
                passwordEncoder.encode("Pass123!"),
                Set.of(role)
        );
        user.setStatus(status);
        return userRepository.save(user);
    }

    private BloodRequest createBloodRequest(BloodGroup bloodGroup, BloodRequestUrgency urgency, BloodRequestStatus status) {
        BloodRequest req = new BloodRequest();
        req.setRequesterUserId(requester.getId());
        req.setBloodGroup(bloodGroup);
        req.setUnitsRequired(2);
        req.setUrgency(urgency);
        req.setStatus(status);
        req.setHospitalName("KEM Hospital");
        req.setHospitalAddress("Acharya Donde Marg, Parel");
        req.setCity("Mumbai");
        req.setState("Maharashtra");
        req.setPostalCode("400012");
        req.setLatitude(REQ_LAT);
        req.setLongitude(REQ_LNG);
        req.setRequiredBy(Instant.now().plus(24, ChronoUnit.HOURS));
        req.setCreatedAt(Instant.now());
        req.setUpdatedAt(Instant.now());
        return bloodRequestRepository.save(req);
    }

    private DonorProfile createDonorProfile(
            User user,
            BloodGroup bloodGroup,
            BloodGroupVerificationStatus verificationStatus,
            DonorAvailabilityStatus availabilityStatus,
            DonorStatus donorStatus,
            LocalDate lastDonationDate,
            Double lat,
            Double lng) {
        DonorProfile dp = new DonorProfile(user.getId(), bloodGroup, availabilityStatus);
        dp.setBloodGroupVerificationStatus(verificationStatus);
        dp.setDonorStatus(donorStatus);
        dp.setLastDonationDate(lastDonationDate);
        dp.setLatitude(lat);
        dp.setLongitude(lng);
        return donorProfileRepository.save(dp);
    }

    @Test
    @DisplayName("Integration: Compatible donor candidates identified, ranked by compatibility & distance")
    void testMatch_CompatibleDonorsFound_AndRankedCorrectly() throws Exception {
        BloodRequest req = createBloodRequest(BloodGroup.A_POSITIVE, BloodRequestUrgency.NORMAL, BloodRequestStatus.OPEN);

        // Donor 1: A+ (EXACT), VERIFIED, nearby (~2 km)
        User u1 = createTestUser("Alice", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        createDonorProfile(u1, BloodGroup.A_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, DonorStatus.ACTIVE, null, 18.9500, 72.8400);

        // Donor 2: O- (COMPATIBLE), VERIFIED, closer (~1 km)
        User u2 = createTestUser("Bob", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        createDonorProfile(u2, BloodGroup.O_NEGATIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, DonorStatus.ACTIVE, null, 18.9450, 72.8380);

        // Donor 3: B+ (INCOMPATIBLE with A+), VERIFIED, nearby
        User u3 = createTestUser("Charlie", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        createDonorProfile(u3, BloodGroup.B_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, DonorStatus.ACTIVE, null, 18.9420, 72.8360);

        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateCount").value(2))
                // Rank #1 must be Exact match Alice (even if Bob is slightly closer)
                .andExpect(jsonPath("$.matches[0].donorDisplayName").value("Alice F."))
                .andExpect(jsonPath("$.matches[0].compatibilityType").value("EXACT"))
                .andExpect(jsonPath("$.matches[0].matchQuality").value("EXCELLENT"))
                // Rank #2 must be Compatible match Bob
                .andExpect(jsonPath("$.matches[1].donorDisplayName").value("Bob F."))
                .andExpect(jsonPath("$.matches[1].compatibilityType").value("COMPATIBLE"))
                .andExpect(jsonPath("$.matches[1].matchQuality").value("GOOD"));
    }

    @Test
    @DisplayName("Integration: Emergency request matching retains CRITICAL urgency in response")
    void testMatch_UrgencyPreserved_CriticalEmergencyRequest() throws Exception {
        BloodRequest req = createBloodRequest(BloodGroup.O_POSITIVE, BloodRequestUrgency.CRITICAL, BloodRequestStatus.OPEN);

        User u1 = createTestUser("David", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        createDonorProfile(u1, BloodGroup.O_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, DonorStatus.ACTIVE, null, 18.9450, 72.8380);

        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urgency").value("CRITICAL"))
                .andExpect(jsonPath("$.bloodGroupRequired").value("O+"))
                .andExpect(jsonPath("$.candidateCount").value(1));
    }

    @Test
    @DisplayName("Integration Filter: Exclude INACTIVE and PAUSED donor profiles")
    void testMatch_FiltersOutInactiveAndPausedDonors() throws Exception {
        BloodRequest req = createBloodRequest(BloodGroup.O_POSITIVE, BloodRequestUrgency.NORMAL, BloodRequestStatus.OPEN);

        // Inactive donor
        User u1 = createTestUser("InactiveDonor", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        createDonorProfile(u1, BloodGroup.O_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, DonorStatus.INACTIVE, null, 18.9450, 72.8380);

        // Paused donor
        User u2 = createTestUser("PausedDonor", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        createDonorProfile(u2, BloodGroup.O_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, DonorStatus.PAUSED, null, 18.9450, 72.8380);

        // Active donor
        User u3 = createTestUser("ActiveDonor", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        createDonorProfile(u3, BloodGroup.O_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, DonorStatus.ACTIVE, null, 18.9450, 72.8380);

        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateCount").value(1))
                .andExpect(jsonPath("$.matches[0].donorDisplayName").value("ActiveDonor F."));
    }

    @Test
    @DisplayName("Integration Filter: Exclude UNAVAILABLE and BUSY donors")
    void testMatch_FiltersOutUnavailableAndBusyDonors() throws Exception {
        BloodRequest req = createBloodRequest(BloodGroup.O_POSITIVE, BloodRequestUrgency.NORMAL, BloodRequestStatus.OPEN);

        User u1 = createTestUser("UnavailDonor", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        createDonorProfile(u1, BloodGroup.O_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.UNAVAILABLE, DonorStatus.ACTIVE, null, 18.9450, 72.8380);

        User u2 = createTestUser("PausedAvailDonor", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        createDonorProfile(u2, BloodGroup.O_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.PAUSED, DonorStatus.ACTIVE, null, 18.9450, 72.8380);

        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateCount").value(0));
    }

    @Test
    @DisplayName("Integration Filter: Exclude donors whose user account is SUSPENDED or DEACTIVATED")
    void testMatch_FiltersOutSuspendedAndDeactivatedUsers() throws Exception {
        BloodRequest req = createBloodRequest(BloodGroup.O_POSITIVE, BloodRequestUrgency.NORMAL, BloodRequestStatus.OPEN);

        User u1 = createTestUser("SuspendedUser", UserRole.ROLE_DONOR, UserStatus.SUSPENDED);
        createDonorProfile(u1, BloodGroup.O_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, DonorStatus.ACTIVE, null, 18.9450, 72.8380);

        User u2 = createTestUser("DeactivatedUser", UserRole.ROLE_DONOR, UserStatus.DEACTIVATED);
        createDonorProfile(u2, BloodGroup.O_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, DonorStatus.ACTIVE, null, 18.9450, 72.8380);

        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateCount").value(0));
    }

    @Test
    @DisplayName("Integration Invariant: Requester is NEVER matched to their own blood request")
    void testMatch_FiltersOutRequester_EvenIfCompatibleDonor() throws Exception {
        BloodRequest req = createBloodRequest(BloodGroup.O_POSITIVE, BloodRequestUrgency.NORMAL, BloodRequestStatus.OPEN);

        // Requester creates their own donor profile with exact matching blood group
        createDonorProfile(requester, BloodGroup.O_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, DonorStatus.ACTIVE, null, REQ_LAT, REQ_LNG);

        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateCount").value(0));
    }

    @Test
    @DisplayName("Integration Filter: Minimum donation interval (90 days) strictly enforced")
    void testMatch_DonationInterval_90DayRule() throws Exception {
        BloodRequest req = createBloodRequest(BloodGroup.O_POSITIVE, BloodRequestUrgency.NORMAL, BloodRequestStatus.OPEN);
        LocalDate today = LocalDate.now();

        // Donated 30 days ago (< 90 days) -> Excluded
        User u1 = createTestUser("RecentDonor", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        createDonorProfile(u1, BloodGroup.O_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, DonorStatus.ACTIVE, today.minusDays(30), 18.9450, 72.8380);

        // Donated 95 days ago (>= 90 days) -> Included
        User u2 = createTestUser("EligibleDonor", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        createDonorProfile(u2, BloodGroup.O_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, DonorStatus.ACTIVE, today.minusDays(95), 18.9450, 72.8380);

        // Never donated (null) -> Included
        User u3 = createTestUser("FirstTimeDonor", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        createDonorProfile(u3, BloodGroup.O_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, DonorStatus.ACTIVE, null, 18.9450, 72.8380);

        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateCount").value(2));
    }

    @Test
    @DisplayName("Integration Filter: Exclude donors with active temporary deferral or medical review required, include expired deferrals")
    void testMatch_EligibilitySelfScreening_DeferralExclusion() throws Exception {
        BloodRequest req = createBloodRequest(BloodGroup.O_POSITIVE, BloodRequestUrgency.NORMAL, BloodRequestStatus.OPEN);

        // Donor with active TEMPORARY_DEFERRAL (+14 days) -> Excluded
        User u1 = createTestUser("DeferredDonor", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        createDonorProfile(u1, BloodGroup.O_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, DonorStatus.ACTIVE, null, 18.9450, 72.8380);
        EligibilitySession session1 = new EligibilitySession();
        session1.setUserId(u1.getId());
        session1.setStatus(org.netra.features.eligibility.entity.SessionStatus.COMPLETED);
        session1.setResult(ResultType.TEMPORARY_DEFERRAL);
        session1.setRuleVersion("INDIA-NBTC-2026-01");
        session1.setEstimatedEligibleDate(LocalDate.now().plusDays(14));
        session1.setStartedAt(Instant.now());
        session1.setCreatedAt(Instant.now());
        session1.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        eligibilitySessionRepository.save(session1);

        // Donor with MEDICAL_REVIEW_REQUIRED -> Excluded
        User u2 = createTestUser("ReviewDonor", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        createDonorProfile(u2, BloodGroup.O_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, DonorStatus.ACTIVE, null, 18.9450, 72.8380);
        EligibilitySession session2 = new EligibilitySession();
        session2.setUserId(u2.getId());
        session2.setStatus(org.netra.features.eligibility.entity.SessionStatus.COMPLETED);
        session2.setResult(ResultType.MEDICAL_REVIEW_REQUIRED);
        session2.setRuleVersion("INDIA-NBTC-2026-01");
        session2.setStartedAt(Instant.now());
        session2.setCreatedAt(Instant.now());
        session2.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        eligibilitySessionRepository.save(session2);

        // Donor with LIKELY_ELIGIBLE -> Included
        User u3 = createTestUser("CleanDonor", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        createDonorProfile(u3, BloodGroup.O_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, DonorStatus.ACTIVE, null, 18.9450, 72.8380);
        EligibilitySession session3 = new EligibilitySession();
        session3.setUserId(u3.getId());
        session3.setStatus(org.netra.features.eligibility.entity.SessionStatus.COMPLETED);
        session3.setResult(ResultType.LIKELY_ELIGIBLE);
        session3.setRuleVersion("INDIA-NBTC-2026-01");
        session3.setStartedAt(Instant.now());
        session3.setCreatedAt(Instant.now());
        session3.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        eligibilitySessionRepository.save(session3);

        // Donor with expired TEMPORARY_DEFERRAL (deferral ended 5 days ago) -> Included
        User u4 = createTestUser("ExpiredDeferralDonor", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        createDonorProfile(u4, BloodGroup.O_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, DonorStatus.ACTIVE, null, 18.9460, 72.8390);
        EligibilitySession session4 = new EligibilitySession();
        session4.setUserId(u4.getId());
        session4.setStatus(org.netra.features.eligibility.entity.SessionStatus.COMPLETED);
        session4.setResult(ResultType.TEMPORARY_DEFERRAL);
        session4.setRuleVersion("INDIA-NBTC-2026-01");
        session4.setEstimatedEligibleDate(LocalDate.now().minusDays(5));
        session4.setStartedAt(Instant.now());
        session4.setCreatedAt(Instant.now());
        session4.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        eligibilitySessionRepository.save(session4);

        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateCount").value(2));
    }

    @Test
    @DisplayName("Integration Filter: Proximity distance and radius boundary filtering")
    void testMatch_ProximityFiltering_RadiusAndMissingCoordinates() throws Exception {
        BloodRequest req = createBloodRequest(BloodGroup.O_POSITIVE, BloodRequestUrgency.NORMAL, BloodRequestStatus.OPEN);

        // Within 10 km (~2 km)
        User u1 = createTestUser("NearDonor", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        createDonorProfile(u1, BloodGroup.O_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, DonorStatus.ACTIVE, null, 18.9550, 72.8450);

        // Far away (~40 km, e.g. Thane/Kalyan coordinates: 19.2183, 72.9781)
        User u2 = createTestUser("FarDonor", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        createDonorProfile(u2, BloodGroup.O_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, DonorStatus.ACTIVE, null, 19.2183, 72.9781);

        // Missing coordinates (lat/lng null)
        User u3 = createTestUser("NoCoordDonor", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        createDonorProfile(u3, BloodGroup.O_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, DonorStatus.ACTIVE, null, null, null);

        // Query with 15km radius
        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .param("radiusKm", "15.0")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateCount").value(1))
                .andExpect(jsonPath("$.matches[0].donorDisplayName").value("NearDonor F."));
    }

    @Test
    @DisplayName("Integration Proximity: Donor in bounding box corner outside exact circular radius is excluded by Stage 2 Haversine")
    void testMatch_BoundingBoxCornerOutsideExactRadius_ExcludedByHaversine() throws Exception {
        BloodRequest req = createBloodRequest(BloodGroup.O_POSITIVE, BloodRequestUrgency.NORMAL, BloodRequestStatus.OPEN);

        // Center: 18.9400, 72.8350. Search radius = 10.0 km.
        // Corner donor: inside bounding box, but ~11.3 km away
        User u1 = createTestUser("CornerDonor", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        createDonorProfile(u1, BloodGroup.O_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, DonorStatus.ACTIVE, null, 19.0121, 72.9112);

        // Inside donor: ~2 km away
        User u2 = createTestUser("InsideDonor", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        createDonorProfile(u2, BloodGroup.O_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, DonorStatus.ACTIVE, null, 18.9500, 72.8400);

        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .param("radiusKm", "10.0")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateCount").value(1))
                .andExpect(jsonPath("$.matches[0].donorDisplayName").value("InsideDonor F."));
    }

    @Test
    @DisplayName("Integration Proximity: Bounding box hard filters exclude faraway donors directly at SQL level")
    void testMatch_BoundingBoxDatabaseFilter_ExcludesFarawayDonors() {
        // Center: 18.9400, 72.8350. Search radius = 10.0 km.
        // latDelta = 10 / 111 = ~0.0901 deg -> maxLat = ~19.0301.
        // Faraway donor at lat 19.2183 is outside maxLat.
        User u1 = createTestUser("FarawaySqlDonor", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        createDonorProfile(u1, BloodGroup.O_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, DonorStatus.ACTIVE, null, 19.2183, 72.9781);

        // Within bounding box donor at lat 18.9500
        User u2 = createTestUser("NearbySqlDonor", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        createDonorProfile(u2, BloodGroup.O_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, DonorStatus.ACTIVE, null, 18.9500, 72.8400);

        double radiusKm = 10.0;
        double latDelta = radiusKm / 111.0;
        double minLat = REQ_LAT - latDelta;
        double maxLat = REQ_LAT + latDelta;
        double cosLat = Math.cos(Math.toRadians(REQ_LAT));
        double lngDelta = radiusKm / (111.0 * cosLat);
        double minLng = REQ_LNG - lngDelta;
        double maxLng = REQ_LNG + lngDelta;

        List<?> candidates = donorMatchingRepository.findCandidateDonors(
                Set.of(BloodGroup.O_POSITIVE),
                requester.getId(),
                minLat, maxLat, minLng, maxLng,
                LocalDate.now().minusDays(90)
        );

        // SQL query directly excluded the faraway donor
        assertEquals(1, candidates.size());
    }

    @Test
    @DisplayName("Integration Filter: Blood group verification status - SELF_REPORTED donors are ALWAYS excluded")
    void testMatch_SelfReportedBloodGroupAlwaysExcluded() throws Exception {
        BloodRequest req = createBloodRequest(BloodGroup.O_POSITIVE, BloodRequestUrgency.NORMAL, BloodRequestStatus.OPEN);

        User u1 = createTestUser("VerifiedDonor", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        createDonorProfile(u1, BloodGroup.O_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, DonorStatus.ACTIVE, null, 18.9500, 72.8400);

        User u2 = createTestUser("SelfReportedDonor", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        createDonorProfile(u2, BloodGroup.O_POSITIVE, BloodGroupVerificationStatus.SELF_REPORTED,
                DonorAvailabilityStatus.AVAILABLE, DonorStatus.ACTIVE, null, 18.9450, 72.8380);

        // Without parameters -> only Verified returned
        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateCount").value(1))
                .andExpect(jsonPath("$.matches[0].donorDisplayName").value("VerifiedDonor F."))
                .andExpect(jsonPath("$.matches[0].bloodGroupVerificationStatus").value("VERIFIED"));

        // If client passes includeSelfReported parameter, it must NOT include self-reported donors
        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .param("includeSelfReported", "true")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateCount").value(1))
                .andExpect(jsonPath("$.matches[0].donorDisplayName").value("VerifiedDonor F."));
    }

    @Test
    @DisplayName("Integration Privacy: safeCandidateId is NEVER exposed in the API response")
    void testMatch_SafeCandidateIdNotExposed() throws Exception {
        BloodRequest req = createBloodRequest(BloodGroup.O_POSITIVE, BloodRequestUrgency.NORMAL, BloodRequestStatus.OPEN);

        User u1 = createTestUser("PrivacyDonor", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        createDonorProfile(u1, BloodGroup.O_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, DonorStatus.ACTIVE, null, 18.9500, 72.8400);

        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateCount").value(1))
                .andExpect(jsonPath("$.matches[0].safeCandidateId").doesNotExist());
    }

    @Test
    @DisplayName("Integration Invariant: Terminal requests (CANCELLED, FULFILLED) cannot be queried for matches")
    void testMatch_TerminalStatusBloodRequest_Rejected() throws Exception {
        BloodRequest req1 = createBloodRequest(BloodGroup.O_POSITIVE, BloodRequestUrgency.NORMAL, BloodRequestStatus.CANCELLED);
        BloodRequest req2 = createBloodRequest(BloodGroup.O_POSITIVE, BloodRequestUrgency.NORMAL, BloodRequestStatus.FULFILLED);

        mockMvc.perform(get("/api/v1/blood-requests/" + req1.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/v1/blood-requests/" + req2.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Integration Lifecycle: Future requiredBy on OPEN request allows matching")
    void testMatch_FutureRequiredBy_MatchingAllowed() throws Exception {
        BloodRequest req = createBloodRequest(BloodGroup.O_POSITIVE, BloodRequestUrgency.NORMAL, BloodRequestStatus.OPEN);
        req.setRequiredBy(Instant.now().plus(24, ChronoUnit.HOURS));
        bloodRequestRepository.save(req);

        User u1 = createTestUser("ValidFutureDonor", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        createDonorProfile(u1, BloodGroup.O_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, DonorStatus.ACTIVE, null, 18.9500, 72.8400);

        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateCount").value(1));
    }

    @Test
    @DisplayName("Integration Lifecycle: Overdue OPEN request (requiredBy <= now) is rejected from matching")
    void testMatch_OverdueOpenRequest_MatchingRejected() throws Exception {
        BloodRequest req = createBloodRequest(BloodGroup.O_POSITIVE, BloodRequestUrgency.NORMAL, BloodRequestStatus.OPEN);
        // Set requiredBy in the past (overdue)
        req.setRequiredBy(Instant.now().minus(2, ChronoUnit.HOURS));
        bloodRequestRepository.save(req);

        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("overdue blood request")));
    }

    @Test
    @DisplayName("Integration Validation: Radius and limit boundaries enforced (400 on out of bounds)")
    void testMatch_BoundsValidation() throws Exception {
        BloodRequest req = createBloodRequest(BloodGroup.O_POSITIVE, BloodRequestUrgency.NORMAL, BloodRequestStatus.OPEN);

        // Radius > 100km
        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .param("radiusKm", "150.0")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Limit > 50
        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .param("limit", "100")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Integration Invariant: Zero side-effects - matching MUST NEVER mutate request, donor profile, or inventory")
    void testMatch_ZeroSideEffects_NoStateMutation() throws Exception {
        BloodRequest req = createBloodRequest(BloodGroup.O_POSITIVE, BloodRequestUrgency.NORMAL, BloodRequestStatus.OPEN);
        Instant originalUpdatedAt = req.getUpdatedAt();

        User u1 = createTestUser("DonorSideEffects", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        DonorProfile dp = createDonorProfile(u1, BloodGroup.O_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, DonorStatus.ACTIVE, null, 18.9450, 72.8380);
        Instant originalDonorUpdatedAt = dp.getUpdatedAt();

        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Verify BloodRequest is untouched
        BloodRequest freshReq = bloodRequestRepository.findById(req.getId()).orElseThrow();
        assertEquals(BloodRequestStatus.OPEN, freshReq.getStatus());
        assertEquals(originalUpdatedAt.toEpochMilli(), freshReq.getUpdatedAt().toEpochMilli());
        assertEquals(2, freshReq.getUnitsRequired());

        // Verify DonorProfile is untouched
        DonorProfile freshDp = donorProfileRepository.findById(dp.getId()).orElseThrow();
        assertEquals(DonorAvailabilityStatus.AVAILABLE, freshDp.getAvailabilityStatus());
        assertEquals(DonorStatus.ACTIVE, freshDp.getDonorStatus());
        assertEquals(originalDonorUpdatedAt.toEpochMilli(), freshDp.getUpdatedAt().toEpochMilli());
    }
}
