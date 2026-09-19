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
import org.netra.features.bloodrequest.service.BloodRequestExpirationService;
import org.netra.features.bloodrequest.service.BloodRequestService;
import org.netra.features.donor.entity.*;
import org.netra.features.donor.repository.DonorProfileRepository;
import org.netra.features.eligibility.repository.EligibilitySessionRepository;
import org.netra.features.matching.dto.CreateDonorMatchRequest;
import org.netra.features.matching.entity.DonorMatch;
import org.netra.features.matching.entity.MatchStatus;
import org.netra.features.matching.repository.DonorMatchRepository;
import org.netra.core.exception.DuplicateResourceException;
import org.netra.features.matching.service.DonorResponseService;
import org.springframework.dao.DataIntegrityViolationException;
import org.netra.features.matching.service.DonorMatchLifecycleService;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DonorResponseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private DonorProfileRepository donorProfileRepository;

    @Autowired
    private DonorMatchRepository donorMatchRepository;

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

    @Autowired
    private BloodRequestService bloodRequestService;

    @Autowired
    private BloodRequestExpirationService bloodRequestExpirationService;

    @Autowired
    private DonorMatchLifecycleService donorMatchLifecycleService;

    @Autowired
    private DonorResponseService donorResponseService;

    private User requester;
    private String requesterToken;

    private User donorUser;
    private DonorProfile donorProfile;
    private String donorToken;

    // Mumbai CST reference
    private static final double REQ_LAT = 18.9400;
    private static final double REQ_LNG = 72.8350;

    @BeforeEach
    void setUp() {
        donorMatchRepository.deleteAll();
        eligibilitySessionRepository.deleteAll();
        donorProfileRepository.deleteAll();
        bloodRequestRepository.deleteAll();
        rateLimitingService.reset();

        requester = createTestUser("req.resp", UserRole.ROLE_RECEIVER, UserStatus.ACTIVE);
        requesterToken = jwtTokenProvider.generateAccessToken(requester.getId(), List.of("ROLE_RECEIVER"));

        donorUser = createTestUser("donor.resp", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        donorToken = jwtTokenProvider.generateAccessToken(donorUser.getId(), List.of("ROLE_DONOR"));

        donorProfile = createDonorProfile(
                donorUser,
                BloodGroup.A_POSITIVE,
                BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE,
                DonorStatus.ACTIVE,
                null,
                18.9450,
                72.8380
        );
    }

    private User createTestUser(String namePrefix, UserRole role, UserStatus status) {
        User user = new User(
                namePrefix + " FullName",
                namePrefix + "." + UUID.randomUUID() + "@netra.org",
                "+9198" + String.format("%08d", Math.abs(UUID.randomUUID().hashCode() % 100000000)),
                passwordEncoder.encode("Test@123456"),
                Set.of(role)
        );
        user.setStatus(status);
        return userRepository.save(user);
    }

    private BloodRequest createBloodRequest(BloodGroup bloodGroup, BloodRequestStatus status, Instant requiredBy) {
        BloodRequest req = new BloodRequest();
        req.setRequesterUserId(requester.getId());
        req.setBloodGroup(bloodGroup);
        req.setUnitsRequired(2);
        req.setUrgency(BloodRequestUrgency.NORMAL);
        req.setStatus(status);
        req.setHospitalName("KEM Hospital");
        req.setHospitalAddress("Acharya Donde Marg, Parel");
        req.setCity("Mumbai");
        req.setState("Maharashtra");
        req.setPostalCode("400012");
        req.setLatitude(REQ_LAT);
        req.setLongitude(REQ_LNG);
        req.setRequiredBy(requiredBy != null ? requiredBy : Instant.now().plus(24, ChronoUnit.HOURS));
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
    @DisplayName("Integration: Authorized requester can create a valid persistent match")
    void testCreateMatch_Success() throws Exception {
        BloodRequest req = createBloodRequest(BloodGroup.A_POSITIVE, BloodRequestStatus.OPEN, null);
        CreateDonorMatchRequest createDto = new CreateDonorMatchRequest(donorProfile.getId());

        mockMvc.perform(post("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bloodRequestId", is(req.getId().toString())))
                .andExpect(jsonPath("$.responseStatus", is("MATCHED")))
                .andExpect(jsonPath("$.donorDisplayName", containsString("donor.resp")))
                .andExpect(jsonPath("$.bloodGroup", is("A+")))
                .andExpect(jsonPath("$.bloodGroupVerificationStatus", is("VERIFIED")))
                .andExpect(jsonPath("$.expiresAt", notNullValue()));

        List<DonorMatch> inDb = donorMatchRepository.findByBloodRequestIdOrderByCreatedAtDesc(req.getId());
        assertEquals(1, inDb.size());
        assertEquals(MatchStatus.MATCHED, inDb.get(0).getResponseStatus());
    }

    @Test
    @DisplayName("Integration: Incompatible blood group donor rejected from match creation")
    void testCreateMatch_IncompatibleDonor_Rejected() throws Exception {
        BloodRequest req = createBloodRequest(BloodGroup.O_NEGATIVE, BloodRequestStatus.OPEN, null); // Only O- can donate to O-
        // donorProfile is A_POSITIVE -> Incompatible
        CreateDonorMatchRequest createDto = new CreateDonorMatchRequest(donorProfile.getId());

        mockMvc.perform(post("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("incompatible")));

        assertTrue(donorMatchRepository.findAll().isEmpty());
    }

    @Test
    @DisplayName("Integration: Unavailable donor rejected from match creation")
    void testCreateMatch_UnavailableDonor_Rejected() throws Exception {
        donorProfile.setAvailabilityStatus(DonorAvailabilityStatus.UNAVAILABLE);
        donorProfileRepository.save(donorProfile);

        BloodRequest req = createBloodRequest(BloodGroup.A_POSITIVE, BloodRequestStatus.OPEN, null);
        CreateDonorMatchRequest createDto = new CreateDonorMatchRequest(donorProfile.getId());

        mockMvc.perform(post("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("not available")));
    }

    @Test
    @DisplayName("Integration: Self-reported (unverified) donor rejected from match creation")
    void testCreateMatch_SelfReportedDonor_Rejected() throws Exception {
        donorProfile.setBloodGroupVerificationStatus(BloodGroupVerificationStatus.SELF_REPORTED);
        donorProfileRepository.save(donorProfile);

        BloodRequest req = createBloodRequest(BloodGroup.A_POSITIVE, BloodRequestStatus.OPEN, null);
        CreateDonorMatchRequest createDto = new CreateDonorMatchRequest(donorProfile.getId());

        mockMvc.perform(post("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("verified blood-group")));
    }

    @Test
    @DisplayName("Integration: Inactive donor user or profile rejected from match creation")
    void testCreateMatch_InactiveDonor_Rejected() throws Exception {
        donorProfile.setDonorStatus(DonorStatus.INACTIVE);
        donorProfileRepository.save(donorProfile);

        BloodRequest req = createBloodRequest(BloodGroup.A_POSITIVE, BloodRequestStatus.OPEN, null);
        CreateDonorMatchRequest createDto = new CreateDonorMatchRequest(donorProfile.getId());

        mockMvc.perform(post("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("not active")));
    }

    @Test
    @DisplayName("Integration: Overdue or Cancelled Blood Request rejected from match creation")
    void testCreateMatch_OverdueOrCancelledRequest_Rejected() throws Exception {
        // Overdue request
        BloodRequest overdueReq = createBloodRequest(BloodGroup.A_POSITIVE, BloodRequestStatus.OPEN, Instant.now().minus(1, ChronoUnit.HOURS));
        CreateDonorMatchRequest createDto = new CreateDonorMatchRequest(donorProfile.getId());

        mockMvc.perform(post("/api/v1/blood-requests/" + overdueReq.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("overdue")));

        // Cancelled request
        BloodRequest cancelledReq = createBloodRequest(BloodGroup.A_POSITIVE, BloodRequestStatus.CANCELLED, Instant.now().plus(24, ChronoUnit.HOURS));

        mockMvc.perform(post("/api/v1/blood-requests/" + cancelledReq.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("CANCELLED")));
    }

    @Test
    @DisplayName("Integration: Duplicate match creation for same request and donor is safely rejected")
    void testCreateMatch_DuplicateMatch_Rejected() throws Exception {
        BloodRequest req = createBloodRequest(BloodGroup.A_POSITIVE, BloodRequestStatus.OPEN, null);
        CreateDonorMatchRequest createDto = new CreateDonorMatchRequest(donorProfile.getId());

        // First creation succeeds
        mockMvc.perform(post("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated());

        // Second creation is rejected as duplicate
        mockMvc.perform(post("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));

        assertEquals(1, donorMatchRepository.findByBloodRequestIdOrderByCreatedAtDesc(req.getId()).size());
    }

    @Test
    @DisplayName("Integration: Donor accept flow (MATCHED -> ACCEPTED) and invariant verification")
    void testDonorAccept_Success_AndInvariantsPreserved() throws Exception {
        BloodRequest req = createBloodRequest(BloodGroup.A_POSITIVE, BloodRequestStatus.OPEN, null);
        DonorMatch match = donorMatchRepository.save(new DonorMatch(req.getId(), donorUser.getId(), Instant.now().plus(24, ChronoUnit.HOURS)));

        mockMvc.perform(post("/api/v1/donor/matches/" + match.getId() + "/accept")
                        .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId", is(match.getId().toString())))
                .andExpect(jsonPath("$.responseStatus", is("ACCEPTED")))
                .andExpect(jsonPath("$.disclaimer", containsString("Final screening is performed by qualified blood-bank staff")));

        DonorMatch updated = donorMatchRepository.findById(match.getId()).orElseThrow();
        assertEquals(MatchStatus.ACCEPTED, updated.getResponseStatus());
        assertNotNull(updated.getRespondedAt());

        // Invariants:
        BloodRequest freshReq = bloodRequestRepository.findById(req.getId()).orElseThrow();
        assertEquals(BloodRequestStatus.OPEN, freshReq.getStatus(), "BloodRequest status must remain OPEN");

        DonorProfile freshProfile = donorProfileRepository.findByUserId(donorUser.getId()).orElseThrow();
        assertNull(freshProfile.getLastDonationDate(), "lastDonationDate must not be modified by accept");
    }

    @Test
    @DisplayName("Integration: Donor decline flow (MATCHED -> DECLINED)")
    void testDonorDecline_Success() throws Exception {
        BloodRequest req = createBloodRequest(BloodGroup.A_POSITIVE, BloodRequestStatus.OPEN, null);
        DonorMatch match = donorMatchRepository.save(new DonorMatch(req.getId(), donorUser.getId(), Instant.now().plus(24, ChronoUnit.HOURS)));

        mockMvc.perform(post("/api/v1/donor/matches/" + match.getId() + "/decline")
                        .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId", is(match.getId().toString())))
                .andExpect(jsonPath("$.responseStatus", is("DECLINED")));

        DonorMatch updated = donorMatchRepository.findById(match.getId()).orElseThrow();
        assertEquals(MatchStatus.DECLINED, updated.getResponseStatus());
        assertNotNull(updated.getRespondedAt());
    }

    @Test
    @DisplayName("Integration: Terminal state cannot reopen; cannot accept already accepted or declined match")
    void testTerminalStateCannotReopen() throws Exception {
        BloodRequest req = createBloodRequest(BloodGroup.A_POSITIVE, BloodRequestStatus.OPEN, null);
        DonorMatch match = donorMatchRepository.save(new DonorMatch(req.getId(), donorUser.getId(), Instant.now().plus(24, ChronoUnit.HOURS)));

        // Accept once
        mockMvc.perform(post("/api/v1/donor/matches/" + match.getId() + "/accept")
                        .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isOk());

        // Accept again -> rejected
        mockMvc.perform(post("/api/v1/donor/matches/" + match.getId() + "/accept")
                        .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isBadRequest());

        // Decline already accepted -> rejected
        mockMvc.perform(post("/api/v1/donor/matches/" + match.getId() + "/decline")
                        .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Integration: Expired or Cancelled match cannot be accepted")
    void testExpiredOrCancelledMatchCannotAccept() throws Exception {
        BloodRequest req1 = createBloodRequest(BloodGroup.A_POSITIVE, BloodRequestStatus.OPEN, null);
        BloodRequest req2 = createBloodRequest(BloodGroup.A_POSITIVE, BloodRequestStatus.OPEN, null);

        // Expired match
        DonorMatch expiredMatch = new DonorMatch(req1.getId(), donorUser.getId(), Instant.now().minus(1, ChronoUnit.MINUTES));
        expiredMatch = donorMatchRepository.save(expiredMatch);

        mockMvc.perform(post("/api/v1/donor/matches/" + expiredMatch.getId() + "/accept")
                        .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("expired")));

        // Cancelled match
        DonorMatch cancelledMatch = new DonorMatch(req2.getId(), donorUser.getId(), Instant.now().plus(24, ChronoUnit.HOURS));
        cancelledMatch.cancel(Instant.now());
        cancelledMatch = donorMatchRepository.save(cancelledMatch);

        mockMvc.perform(post("/api/v1/donor/matches/" + cancelledMatch.getId() + "/accept")
                        .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("CANCELLED")));
    }

    @Test
    @DisplayName("Integration: Blood Request cancellation cancels active MATCHED matches")
    void testRequestCancellation_CancelsActiveMatches() throws Exception {
        BloodRequest req = createBloodRequest(BloodGroup.A_POSITIVE, BloodRequestStatus.OPEN, null);
        DonorMatch activeMatch = donorMatchRepository.save(new DonorMatch(req.getId(), donorUser.getId(), Instant.now().plus(24, ChronoUnit.HOURS)));

        // Cancel the blood request via authorized endpoint
        mockMvc.perform(post("/api/v1/blood-requests/" + req.getId() + "/cancel")
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isOk());

        DonorMatch updated = donorMatchRepository.findById(activeMatch.getId()).orElseThrow();
        assertEquals(MatchStatus.CANCELLED, updated.getResponseStatus());
    }

    @Test
    @DisplayName("Integration: Blood Request expiration expires active MATCHED matches")
    void testRequestExpiration_ExpiresActiveMatches() throws Exception {
        BloodRequest req = createBloodRequest(BloodGroup.A_POSITIVE, BloodRequestStatus.OPEN, Instant.now().minus(1, ChronoUnit.MINUTES));
        DonorMatch activeMatch = donorMatchRepository.save(new DonorMatch(req.getId(), donorUser.getId(), Instant.now().plus(24, ChronoUnit.HOURS)));

        // Run expiration service
        bloodRequestExpirationService.processExpirations(Instant.now());

        DonorMatch updated = donorMatchRepository.findById(activeMatch.getId()).orElseThrow();
        assertEquals(MatchStatus.EXPIRED, updated.getResponseStatus());
    }

    @Test
    @DisplayName("Integration: expireOverdueMatches does not turn matches into EXPIRED if parent request is CANCELLED")
    void testExpireOverdueMatches_ExcludesCancelledRequestMatches() throws Exception {
        // Parent request is CANCELLED
        BloodRequest req = createBloodRequest(BloodGroup.A_POSITIVE, BloodRequestStatus.CANCELLED, null);
        // Match has expiresAt in past
        DonorMatch match = donorMatchRepository.save(new DonorMatch(
                req.getId(), donorUser.getId(), Instant.now().minus(2, ChronoUnit.HOURS), Instant.now().minus(1, ChronoUnit.HOURS)));

        // Run background match expiration
        donorMatchLifecycleService.expireOverdueMatches(Instant.now());

        DonorMatch after = donorMatchRepository.findById(match.getId()).orElseThrow();
        assertEquals(MatchStatus.MATCHED, after.getResponseStatus(),
                "Match belonging to CANCELLED blood request must NOT be transitioned to EXPIRED by background expiration");
    }

    @Test
    @DisplayName("Integration: expireActiveMatchesForRequest is conditional on parent request being EXPIRED")
    void testExpireActiveMatchesForRequest_ConditionalOnRequestExpired() throws Exception {
        // 1. OPEN request: must not expire matches
        BloodRequest openReq = createBloodRequest(BloodGroup.A_POSITIVE, BloodRequestStatus.OPEN, null);
        DonorMatch openMatch = donorMatchRepository.save(new DonorMatch(openReq.getId(), donorUser.getId(), Instant.now().plus(24, ChronoUnit.HOURS)));

        int openResult = donorMatchLifecycleService.expireActiveMatchesForRequest(openReq.getId());
        assertEquals(0, openResult, "Must not expire matches for OPEN request");
        assertEquals(MatchStatus.MATCHED, donorMatchRepository.findById(openMatch.getId()).orElseThrow().getResponseStatus());

        // 2. EXPIRED request: must expire active matches
        BloodRequest expiredReq = createBloodRequest(BloodGroup.A_POSITIVE, BloodRequestStatus.EXPIRED, Instant.now().minus(1, ChronoUnit.HOURS));
        DonorMatch expiredMatch = donorMatchRepository.save(new DonorMatch(expiredReq.getId(), donorUser.getId(), Instant.now().plus(24, ChronoUnit.HOURS)));

        int expiredResult = donorMatchLifecycleService.expireActiveMatchesForRequest(expiredReq.getId());
        assertEquals(1, expiredResult, "Must expire active matches for EXPIRED request");
        assertEquals(MatchStatus.EXPIRED, donorMatchRepository.findById(expiredMatch.getId()).orElseThrow().getResponseStatus());
    }

    @Test
    @DisplayName("Integration: Requester can retrieve persistent match-responses for their request")
    void testGetMatchResponses_Success() throws Exception {
        BloodRequest req = createBloodRequest(BloodGroup.A_POSITIVE, BloodRequestStatus.OPEN, null);
        donorMatchRepository.save(new DonorMatch(req.getId(), donorUser.getId(), Instant.now().plus(24, ChronoUnit.HOURS)));

        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId() + "/match-responses")
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].donorDisplayName", containsString("donor.resp")))
                .andExpect(jsonPath("$[0].responseStatus", is("MATCHED")))
                .andExpect(jsonPath("$[0].bloodGroup", is("A+")));
    }

    @Test
    @DisplayName("Integration: Decline state validation rejects decline if request is cancelled, expired, or overdue")
    void testDeclineStateValidation_CancelledOrExpired_Rejected() throws Exception {
        // 1. Cancelled blood request
        BloodRequest reqCancelled = createBloodRequest(BloodGroup.A_POSITIVE, BloodRequestStatus.CANCELLED, null);
        DonorMatch match1 = donorMatchRepository.save(new DonorMatch(reqCancelled.getId(), donorUser.getId(), Instant.now().plus(24, ChronoUnit.HOURS)));

        mockMvc.perform(post("/api/v1/donor/matches/" + match1.getId() + "/decline")
                        .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsStringIgnoringCase("cancelled")));

        // 2. Overdue blood request (requiredBy in past)
        BloodRequest reqOverdue = createBloodRequest(BloodGroup.A_POSITIVE, BloodRequestStatus.OPEN, Instant.now().minus(1, ChronoUnit.HOURS));
        DonorMatch match2 = donorMatchRepository.save(new DonorMatch(reqOverdue.getId(), donorUser.getId(), Instant.now().plus(24, ChronoUnit.HOURS)));

        mockMvc.perform(post("/api/v1/donor/matches/" + match2.getId() + "/decline")
                        .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("overdue")));

        // 3. Expired match (expiresAt in past)
        BloodRequest reqActive = createBloodRequest(BloodGroup.A_POSITIVE, BloodRequestStatus.OPEN, null);
        DonorMatch match3 = donorMatchRepository.save(new DonorMatch(reqActive.getId(), donorUser.getId(), Instant.now().minus(1, ChronoUnit.MINUTES)));

        mockMvc.perform(post("/api/v1/donor/matches/" + match3.getId() + "/decline")
                        .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("expired")));
    }

    @Test
    @DisplayName("Integration: Raw distance boundary enforces max radius strictly before rounding")
    void testDistanceBoundary_StrictRawRadiusEnforced() throws Exception {
        BloodRequest req = createBloodRequest(BloodGroup.A_POSITIVE, BloodRequestStatus.OPEN, null);

        // Distance > 100.0 km (e.g. ~118 km away from 18.9400 CST)
        User farDonorUser = createTestUser("fardonor", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        DonorProfile farProfile = createDonorProfile(
                farDonorUser,
                BloodGroup.A_POSITIVE,
                BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE,
                DonorStatus.ACTIVE,
                null,
                20.0000, // ~118 km north (> 100.0 km max radius)
                72.8350
        );

        CreateDonorMatchRequest createDto = new CreateDonorMatchRequest(farProfile.getId());

        mockMvc.perform(post("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("exceeds maximum matching radius")));
    }

    @Test
    @DisplayName("Integration: Abuse protection enforces max active matches per request limit")
    void testAbuseProtection_MaxActiveMatchesLimit() throws Exception {
        BloodRequest req = createBloodRequest(BloodGroup.A_POSITIVE, BloodRequestStatus.OPEN, null);

        // Populate 10 active matches (the limit defined in application.yml)
        for (int i = 0; i < 10; i++) {
            User u = createTestUser("bulkdonor" + i, UserRole.ROLE_DONOR, UserStatus.ACTIVE);
            donorMatchRepository.save(new DonorMatch(req.getId(), u.getId(), Instant.now().plus(24, ChronoUnit.HOURS)));
        }

        // Attempting to create the 11th match must fail with abuse limit
        CreateDonorMatchRequest createDto = new CreateDonorMatchRequest(donorProfile.getId());

        mockMvc.perform(post("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("active pending matches")));
    }

    @Test
    @DisplayName("Integration: Rate limiting blocks excessive match creation calls")
    void testAbuseProtection_RateLimitingEnforced() throws Exception {
        // Send 20 requests (configured limit is 20/min) across 2 requests so per-request limit (10) isn't exceeded
        BloodRequest reqA = createBloodRequest(BloodGroup.A_POSITIVE, BloodRequestStatus.OPEN, null);
        BloodRequest reqB = createBloodRequest(BloodGroup.A_POSITIVE, BloodRequestStatus.OPEN, null);

        for (int i = 0; i < 20; i++) {
            User u = createTestUser("rl.donor" + i, UserRole.ROLE_DONOR, UserStatus.ACTIVE);
            DonorProfile dp = createDonorProfile(
                    u,
                    BloodGroup.A_POSITIVE,
                    BloodGroupVerificationStatus.VERIFIED,
                    DonorAvailabilityStatus.AVAILABLE,
                    DonorStatus.ACTIVE,
                    null,
                    18.9450,
                    72.8380
            );

            UUID targetReqId = (i < 10) ? reqA.getId() : reqB.getId();
            CreateDonorMatchRequest createDto = new CreateDonorMatchRequest(dp.getId());
            mockMvc.perform(post("/api/v1/blood-requests/" + targetReqId + "/matches")
                            .header("Authorization", "Bearer " + requesterToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andExpect(status().isCreated());
        }

        // 21st request must trigger 429 Too Many Requests
        User extraUser = createTestUser("rl.extra", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        DonorProfile extraDp = createDonorProfile(
                extraUser,
                BloodGroup.A_POSITIVE,
                BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE,
                DonorStatus.ACTIVE,
                null,
                18.9450,
                72.8380
        );
        CreateDonorMatchRequest extraDto = new CreateDonorMatchRequest(extraDp.getId());

        mockMvc.perform(post("/api/v1/blood-requests/" + reqA.getId() + "/matches")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(extraDto)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message", containsString("Too many match creation requests")));
    }

    @Test
    @DisplayName("Integration: Data Integrity Error Handling distinguishes uq_donor_matches_request_donor from generic DVI")
    void testDataIntegrityErrorHandling_DistinguishesUniqueConstraint() {
        DataIntegrityViolationException uniqueDvi = new DataIntegrityViolationException(
                "could not execute statement",
                new org.hibernate.exception.ConstraintViolationException(
                        "Unique index or primary key violation: \"UQ_DONOR_MATCHES_REQUEST_DONOR_INDEX_E ON PUBLIC.DONOR_MATCHES(BLOOD_REQUEST_ID, DONOR_USER_ID)\"",
                        null,
                        "uq_donor_matches_request_donor"
                )
        );

        DataIntegrityViolationException fkDvi = new DataIntegrityViolationException(
                "could not execute statement",
                new org.hibernate.exception.ConstraintViolationException(
                        "Referential integrity constraint violation: \"FK_DONOR_MATCHES_REQUEST: PUBLIC.DONOR_MATCHES FOREIGN KEY(BLOOD_REQUEST_ID) REFERENCES PUBLIC.BLOOD_REQUESTS(ID)\"",
                        null,
                        "fk_donor_matches_request"
                )
        );

        try {
            var method = DonorResponseService.class.getDeclaredMethod("isUniqueMatchConstraintViolation", DataIntegrityViolationException.class);
            method.setAccessible(true);
            boolean isUnique = (boolean) method.invoke(donorResponseService, uniqueDvi);
            boolean isFk = (boolean) method.invoke(donorResponseService, fkDvi);

            assertTrue(isUnique, "Must recognize uq_donor_matches_request_donor violation");
            assertFalse(isFk, "Must not treat foreign key violation as duplicate resource exception");
        } catch (Exception e) {
            fail("Reflection call failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Integration: DonorProfileRepository.findAllByUserIdIn fetches only requested user IDs")
    void testFindAllByUserIdIn_FetchesOnlyRequestedUserIds() {
        User otherUser = createTestUser("other.donor", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        createDonorProfile(otherUser, BloodGroup.B_POSITIVE, BloodGroupVerificationStatus.VERIFIED,
                DonorAvailabilityStatus.AVAILABLE, DonorStatus.ACTIVE, null, 18.9450, 72.8380);

        List<DonorProfile> result = donorProfileRepository.findAllByUserIdIn(List.of(donorUser.getId()));
        assertEquals(1, result.size());
        assertEquals(donorUser.getId(), result.get(0).getUserId());
    }
}
