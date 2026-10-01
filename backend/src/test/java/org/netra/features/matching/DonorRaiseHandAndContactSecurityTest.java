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
import org.netra.features.matching.entity.DonorMatch;
import org.netra.features.matching.entity.MatchStatus;
import org.netra.features.matching.repository.DonorMatchRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DonorRaiseHandAndContactSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private DonorProfileRepository donorProfileRepository;

    @Autowired
    private DonorMatchRepository donorMatchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RateLimitingService rateLimitingService;

    private User requester;
    private String requesterToken;

    private User donorUser;
    private String donorToken;

    private User unrelatedUserC;
    private String unrelatedToken;

    private BloodRequest testRequest;

    @BeforeEach
    void setUp() {
        donorMatchRepository.deleteAll();
        donorProfileRepository.deleteAll();
        bloodRequestRepository.deleteAll();
        rateLimitingService.reset();

        requester = createTestUser("req.raise", UserRole.ROLE_RECEIVER, UserStatus.ACTIVE);
        requesterToken = jwtTokenProvider.generateAccessToken(requester.getId(), List.of("ROLE_RECEIVER"));

        donorUser = createTestUser("donor.raise", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        donorToken = jwtTokenProvider.generateAccessToken(donorUser.getId(), List.of("ROLE_DONOR"));

        unrelatedUserC = createTestUser("userc.raise", UserRole.ROLE_RECEIVER, UserStatus.ACTIVE);
        unrelatedToken = jwtTokenProvider.generateAccessToken(unrelatedUserC.getId(), List.of("ROLE_RECEIVER"));

        createDonorProfile(donorUser, BloodGroup.A_POSITIVE, 18.9450, 72.8380);

        testRequest = new BloodRequest();
        testRequest.setRequesterUserId(requester.getId());
        testRequest.setBloodGroup(BloodGroup.A_POSITIVE);
        testRequest.setUnitsRequired(2);
        testRequest.setUnitsFulfilled(0);
        testRequest.setStatus(BloodRequestStatus.OPEN);
        testRequest.setUrgency(BloodRequestUrgency.NORMAL);
        testRequest.setHospitalName("SSKM Hospital Kolkata");
        testRequest.setHospitalAddress("110 Harish Mukherjee Road");
        testRequest.setCity("Kolkata");
        testRequest.setState("West Bengal");
        testRequest.setPostalCode("700020");
        testRequest.setLatitude(22.5385);
        testRequest.setLongitude(88.3426);
        testRequest.setRequiredBy(Instant.now().plus(24, ChronoUnit.HOURS));
        testRequest.setCreatedAt(Instant.now());
        testRequest.setUpdatedAt(Instant.now());
        testRequest = bloodRequestRepository.save(testRequest);
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

    private DonorProfile createDonorProfile(User user, BloodGroup bloodGroup, Double lat, Double lng) {
        DonorProfile dp = new DonorProfile(user.getId(), bloodGroup, DonorAvailabilityStatus.AVAILABLE);
        dp.setBloodGroupVerificationStatus(BloodGroupVerificationStatus.VERIFIED);
        dp.setDonorStatus(DonorStatus.ACTIVE);
        dp.setLatitude(lat);
        dp.setLongitude(lng);
        return donorProfileRepository.save(dp);
    }

    @Test
    @DisplayName("Donor can raise hand for open blood request and creates match in MATCHED status")
    void donor_canRaiseHand_forOpenRequest() throws Exception {
        mockMvc.perform(post("/api/v1/blood-requests/" + testRequest.getId() + "/raise-hand")
                        .header("Authorization", "Bearer " + donorToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bloodRequestId").value(testRequest.getId().toString()))
                .andExpect(jsonPath("$.responseStatus").value("MATCHED"))
                .andExpect(jsonPath("$.hospitalName").value("SSKM Hospital Kolkata"));

        var matches = donorMatchRepository.findAll();
        assertEquals(1, matches.size());
        assertEquals(MatchStatus.MATCHED, matches.get(0).getResponseStatus());
    }

    @Test
    @DisplayName("Duplicate raise hand returns 409 Conflict")
    void donor_cannotRaiseHand_twiceOnSameRequest() throws Exception {
        // First raise hand succeeds
        mockMvc.perform(post("/api/v1/blood-requests/" + testRequest.getId() + "/raise-hand")
                        .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isCreated());

        // Second raise hand returns 409 Conflict
        mockMvc.perform(post("/api/v1/blood-requests/" + testRequest.getId() + "/raise-hand")
                        .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Requester cannot offer help on own blood request")
    void requester_cannotRaiseHand_onOwnBloodRequest() throws Exception {
        mockMvc.perform(post("/api/v1/blood-requests/" + testRequest.getId() + "/raise-hand")
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Requester can accept helper and transition match to ACCEPTED")
    void requester_canAcceptHelper() throws Exception {
        // Donor raises hand
        mockMvc.perform(post("/api/v1/blood-requests/" + testRequest.getId() + "/raise-hand")
                        .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isCreated());

        DonorMatch match = donorMatchRepository.findAll().get(0);

        // Requester accepts match
        mockMvc.perform(post("/api/v1/blood-requests/" + testRequest.getId() + "/matches/" + match.getId() + "/accept")
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseStatus").value("ACCEPTED"));

        DonorMatch updatedMatch = donorMatchRepository.findById(match.getId()).orElseThrow();
        assertEquals(MatchStatus.ACCEPTED, updatedMatch.getResponseStatus());
    }

    @Test
    @DisplayName("Requester can decline helper and transition match to DECLINED")
    void requester_canDeclineHelper() throws Exception {
        // Donor raises hand
        mockMvc.perform(post("/api/v1/blood-requests/" + testRequest.getId() + "/raise-hand")
                        .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isCreated());

        DonorMatch match = donorMatchRepository.findAll().get(0);

        // Requester declines match
        mockMvc.perform(post("/api/v1/blood-requests/" + testRequest.getId() + "/matches/" + match.getId() + "/decline")
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseStatus").value("DECLINED"));

        DonorMatch updatedMatch = donorMatchRepository.findById(match.getId()).orElseThrow();
        assertEquals(MatchStatus.DECLINED, updatedMatch.getResponseStatus());
    }

    @Test
    @DisplayName("Contact details cannot be viewed before acceptance (HTTP 400)")
    void contactDetails_cannotBeViewed_beforeAcceptance() throws Exception {
        // Donor raises hand (MATCHED status)
        mockMvc.perform(post("/api/v1/blood-requests/" + testRequest.getId() + "/raise-hand")
                        .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isCreated());

        DonorMatch match = donorMatchRepository.findAll().get(0);

        // Attempting to fetch contact before acceptance returns 400
        mockMvc.perform(get("/api/v1/donor/matches/" + match.getId() + "/contact")
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Unauthenticated request for contact returns 401 Unauthorized")
    void contactDetails_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/donor/matches/" + UUID.randomUUID() + "/contact"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Third-party User C cannot access contact details (HTTP 403 Forbidden)")
    void contactDetails_unrelatedUserC_returns403() throws Exception {
        // Donor raises hand and Requester accepts
        mockMvc.perform(post("/api/v1/blood-requests/" + testRequest.getId() + "/raise-hand")
                        .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isCreated());

        DonorMatch match = donorMatchRepository.findAll().get(0);

        mockMvc.perform(post("/api/v1/blood-requests/" + testRequest.getId() + "/matches/" + match.getId() + "/accept")
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isOk());

        // User C tries to access contact details -> 403 Forbidden
        mockMvc.perform(get("/api/v1/donor/matches/" + match.getId() + "/contact")
                        .header("Authorization", "Bearer " + unrelatedToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Both Requester and Accepted Donor can access verified contact details (HTTP 200)")
    void contactDetails_bothRequesterAndDonor_canAccess_afterAcceptance() throws Exception {
        // Donor raises hand
        mockMvc.perform(post("/api/v1/blood-requests/" + testRequest.getId() + "/raise-hand")
                        .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isCreated());

        DonorMatch match = donorMatchRepository.findAll().get(0);

        // Requester accepts
        mockMvc.perform(post("/api/v1/blood-requests/" + testRequest.getId() + "/matches/" + match.getId() + "/accept")
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isOk());

        // 1. Requester accesses contact
        mockMvc.perform(get("/api/v1/donor/matches/" + match.getId() + "/contact")
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donorPhone").value(donorUser.getPhone()))
                .andExpect(jsonPath("$.donorName").value(donorUser.getFullName()))
                .andExpect(jsonPath("$.hospitalName").value("SSKM Hospital Kolkata"));

        // 2. Donor accesses contact
        mockMvc.perform(get("/api/v1/donor/matches/" + match.getId() + "/contact")
                        .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requesterPhone").value(requester.getPhone()))
                .andExpect(jsonPath("$.requesterName").value(requester.getFullName()))
                .andExpect(jsonPath("$.hospitalName").value("SSKM Hospital Kolkata"));
    }

    @Test
    @DisplayName("Public blood request summary contains unitsFulfilled and helperCount")
    void bloodRequestSummary_containsFulfillmentAndHelperCounts() throws Exception {
        // Initial state: 0 fulfilled, 0 helpers
        mockMvc.perform(get("/api/v1/blood-requests/" + testRequest.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitsRequired").value(2))
                .andExpect(jsonPath("$.unitsFulfilled").value(0))
                .andExpect(jsonPath("$.helperCount").value(0));

        // Donor raises hand
        mockMvc.perform(post("/api/v1/blood-requests/" + testRequest.getId() + "/raise-hand")
                        .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isCreated());

        // Now helperCount is 1
        mockMvc.perform(get("/api/v1/blood-requests/" + testRequest.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.helperCount").value(1));
    }
}
