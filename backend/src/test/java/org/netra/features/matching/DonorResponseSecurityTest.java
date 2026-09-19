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
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.netra.features.matching.dto.CreateDonorMatchRequest;

@SpringBootTest
@AutoConfigureMockMvc
class DonorResponseSecurityTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RateLimitingService rateLimitingService;

    private User requester1;
    private String requester1Token;

    private User requester2;
    private String requester2Token;

    private User admin;
    private String adminToken;

    private User donor1;
    private String donor1Token;

    private User donor2;
    private String donor2Token;

    @BeforeEach
    void setUp() {
        donorMatchRepository.deleteAll();
        donorProfileRepository.deleteAll();
        bloodRequestRepository.deleteAll();
        rateLimitingService.reset();

        requester1 = createTestUser("req1.sec", UserRole.ROLE_RECEIVER, UserStatus.ACTIVE);
        requester1Token = jwtTokenProvider.generateAccessToken(requester1.getId(), List.of("ROLE_RECEIVER"));

        requester2 = createTestUser("req2.sec", UserRole.ROLE_RECEIVER, UserStatus.ACTIVE);
        requester2Token = jwtTokenProvider.generateAccessToken(requester2.getId(), List.of("ROLE_RECEIVER"));

        admin = createTestUser("admin.sec", UserRole.ROLE_ADMIN, UserStatus.ACTIVE);
        adminToken = jwtTokenProvider.generateAccessToken(admin.getId(), List.of("ROLE_ADMIN"));

        donor1 = createTestUser("donor1.sec", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        donor1Token = jwtTokenProvider.generateAccessToken(donor1.getId(), List.of("ROLE_DONOR"));

        donor2 = createTestUser("donor2.sec", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        donor2Token = jwtTokenProvider.generateAccessToken(donor2.getId(), List.of("ROLE_DONOR"));

        createDonorProfile(donor1, BloodGroup.A_POSITIVE, 18.9450, 72.8380);
        createDonorProfile(donor2, BloodGroup.A_POSITIVE, 18.9500, 72.8400);
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

    private BloodRequest createBloodRequest(User reqOwner) {
        BloodRequest req = new BloodRequest();
        req.setRequesterUserId(reqOwner.getId());
        req.setBloodGroup(BloodGroup.A_POSITIVE);
        req.setUnitsRequired(2);
        req.setUrgency(BloodRequestUrgency.NORMAL);
        req.setStatus(BloodRequestStatus.OPEN);
        req.setHospitalName("KEM Hospital");
        req.setHospitalAddress("Acharya Donde Marg, Parel");
        req.setCity("Mumbai");
        req.setState("Maharashtra");
        req.setPostalCode("400012");
        req.setLatitude(18.9400);
        req.setLongitude(72.8350);
        req.setRequiredBy(Instant.now().plus(24, ChronoUnit.HOURS));
        req.setCreatedAt(Instant.now());
        req.setUpdatedAt(Instant.now());
        return bloodRequestRepository.save(req);
    }

    @Test
    @DisplayName("Security: Unauthenticated access to donor and requester matching endpoints is rejected with 401")
    void testUnauthenticated_Rejected() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/donor/matches"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/donor/matches/" + randomId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/donor/matches/" + randomId + "/accept"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/donor/matches/" + randomId + "/decline"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/blood-requests/" + randomId + "/match-responses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security: Donor can access own matches, but cannot access another donor's matches")
    void testDonorIsolation() throws Exception {
        BloodRequest req = createBloodRequest(requester1);
        DonorMatch matchForDonor1 = donorMatchRepository.save(new DonorMatch(req.getId(), donor1.getId(), Instant.now().plus(24, ChronoUnit.HOURS)));

        // Donor 1 can access own match
        mockMvc.perform(get("/api/v1/donor/matches/" + matchForDonor1.getId())
                        .header("Authorization", "Bearer " + donor1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId", is(matchForDonor1.getId().toString())));

        // Donor 2 cannot access Donor 1's match
        mockMvc.perform(get("/api/v1/donor/matches/" + matchForDonor1.getId())
                        .header("Authorization", "Bearer " + donor2Token))
                .andExpect(status().isForbidden());

        // Donor 2 cannot accept Donor 1's match
        mockMvc.perform(post("/api/v1/donor/matches/" + matchForDonor1.getId() + "/accept")
                        .header("Authorization", "Bearer " + donor2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Requester can access matches for own request, but cannot access another requester's matches")
    void testRequesterIsolation() throws Exception {
        BloodRequest req1 = createBloodRequest(requester1);
        donorMatchRepository.save(new DonorMatch(req1.getId(), donor1.getId(), Instant.now().plus(24, ChronoUnit.HOURS)));

        // Requester 1 can access own match responses
        mockMvc.perform(get("/api/v1/blood-requests/" + req1.getId() + "/match-responses")
                        .header("Authorization", "Bearer " + requester1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Requester 2 cannot access Requester 1's match responses
        mockMvc.perform(get("/api/v1/blood-requests/" + req1.getId() + "/match-responses")
                        .header("Authorization", "Bearer " + requester2Token))
                .andExpect(status().isForbidden());

        // Admin can access Requester 1's match responses
        mockMvc.perform(get("/api/v1/blood-requests/" + req1.getId() + "/match-responses")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("Privacy: Requester match-responses never expose donor private phone, email, coordinates, or health records")
    void testPrivacy_NoLeakageInRequesterView() throws Exception {
        BloodRequest req = createBloodRequest(requester1);
        donorMatchRepository.save(new DonorMatch(req.getId(), donor1.getId(), Instant.now().plus(24, ChronoUnit.HOURS)));

        MvcResult result = mockMvc.perform(get("/api/v1/blood-requests/" + req.getId() + "/match-responses")
                        .header("Authorization", "Bearer " + requester1Token))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();

        // Must NOT contain sensitive identifiers or exact coordinates
        assertFalse(json.contains(donor1.getEmail()), "Must not leak donor email");
        assertFalse(json.contains(donor1.getPhone()), "Must not leak donor phone");
        assertFalse(json.contains("18.945"), "Must not leak exact latitude");
        assertFalse(json.contains("72.838"), "Must not leak exact longitude");
        assertFalse(json.contains("eligibilitySession"), "Must not leak eligibility questionnaire");
        assertFalse(json.contains("auditMetadata"), "Must not leak audit metadata");
    }

    @Test
    @DisplayName("Security: Requester can create a persistent match for their own blood request (201)")
    void testRequesterMatchCreation_OwnRequest_Allowed() throws Exception {
        BloodRequest req = createBloodRequest(requester1);
        DonorProfile dp = donorProfileRepository.findByUserId(donor1.getId()).orElseThrow();
        CreateDonorMatchRequest dto = new CreateDonorMatchRequest(dp.getId());

        mockMvc.perform(post("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requester1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bloodRequestId", is(req.getId().toString())))
                .andExpect(jsonPath("$.responseStatus", is("MATCHED")));
    }

    @Test
    @DisplayName("Security: Requester cannot create a match for another requester's blood request (403)")
    void testRequesterMatchCreation_OtherRequester_Forbidden() throws Exception {
        BloodRequest req = createBloodRequest(requester1);
        DonorProfile dp = donorProfileRepository.findByUserId(donor1.getId()).orElseThrow();
        CreateDonorMatchRequest dto = new CreateDonorMatchRequest(dp.getId());

        mockMvc.perform(post("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requester2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Administrator can create a match for any blood request (201)")
    void testRequesterMatchCreation_Admin_Allowed() throws Exception {
        BloodRequest req = createBloodRequest(requester1);
        DonorProfile dp = donorProfileRepository.findByUserId(donor1.getId()).orElseThrow();
        CreateDonorMatchRequest dto = new CreateDonorMatchRequest(dp.getId());

        mockMvc.perform(post("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bloodRequestId", is(req.getId().toString())));
    }

    @Test
    @DisplayName("Security: Incompatible donor cannot bypass validation when creating persistent match (400)")
    void testRequesterMatchCreation_IncompatibleDonor_BadRequest() throws Exception {
        BloodRequest req = createBloodRequest(requester1); // Requires A+
        User bDonorUser = createTestUser("bdonor.sec", UserRole.ROLE_DONOR, UserStatus.ACTIVE);
        DonorProfile bDonorProfile = createDonorProfile(bDonorUser, BloodGroup.B_POSITIVE, 18.9450, 72.8380);

        CreateDonorMatchRequest dto = new CreateDonorMatchRequest(bDonorProfile.getId());

        mockMvc.perform(post("/api/v1/blood-requests/" + req.getId() + "/matches")
                        .header("Authorization", "Bearer " + requester1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("incompatible")));
    }
}
