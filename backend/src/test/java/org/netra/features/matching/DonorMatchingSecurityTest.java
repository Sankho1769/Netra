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
import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.donor.entity.BloodGroupVerificationStatus;
import org.netra.features.donor.entity.DonorAvailabilityStatus;
import org.netra.features.donor.entity.DonorProfile;
import org.netra.features.donor.entity.DonorStatus;
import org.netra.features.donor.repository.DonorProfileRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DonorMatchingSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private DonorProfileRepository donorProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RateLimitingService rateLimitingService;

    private User requester;
    private User attacker;
    private User admin;
    private User suspendedUser;
    private BloodRequest bloodRequest;

    @BeforeEach
    void setUp() {
        donorProfileRepository.deleteAll();
        bloodRequestRepository.deleteAll();
        rateLimitingService.reset();

        requester = createTestUser("requester", UserRole.ROLE_RECEIVER, UserStatus.ACTIVE);
        attacker = createTestUser("attacker", UserRole.ROLE_RECEIVER, UserStatus.ACTIVE);
        admin = createTestUser("admin", UserRole.ROLE_ADMIN, UserStatus.ACTIVE);
        suspendedUser = createTestUser("suspended", UserRole.ROLE_RECEIVER, UserStatus.SUSPENDED);

        // Create standard blood request owned by requester (Mumbai center: 19.0760, 72.8777)
        bloodRequest = new BloodRequest();
        bloodRequest.setRequesterUserId(requester.getId());
        bloodRequest.setBloodGroup(BloodGroup.O_POSITIVE);
        bloodRequest.setUnitsRequired(2);
        bloodRequest.setUrgency(BloodRequestUrgency.NORMAL);
        bloodRequest.setStatus(BloodRequestStatus.OPEN);
        bloodRequest.setHospitalName("City Care Hospital");
        bloodRequest.setHospitalAddress("123 Health Ave");
        bloodRequest.setCity("Mumbai");
        bloodRequest.setState("Maharashtra");
        bloodRequest.setPostalCode("400001");
        bloodRequest.setLatitude(19.0760);
        bloodRequest.setLongitude(72.8777);
        bloodRequest.setRequiredBy(Instant.now().plus(48, ChronoUnit.HOURS));
        bloodRequest.setCreatedAt(Instant.now());
        bloodRequest.setUpdatedAt(Instant.now());
        bloodRequest = bloodRequestRepository.save(bloodRequest);
    }

    private User createTestUser(String prefix, UserRole role, UserStatus status) {
        String email = prefix + "." + UUID.randomUUID() + "@netra.org";
        User user = new User(
                prefix + " Name",
                email,
                "+919876543210",
                passwordEncoder.encode("SecurePass123!"),
                Set.of(role)
        );
        user.setStatus(status);
        return userRepository.save(user);
    }

    @Test
    @DisplayName("Security: Unauthenticated request must return 401 Unauthorized")
    void testGetMatches_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/blood-requests/" + bloodRequest.getId() + "/matches")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security BOLA / IDOR: Unauthorized user querying another user's blood request must return 403 Forbidden")
    void testGetMatches_UnauthorizedUser_Returns403() throws Exception {
        String attackerToken = jwtTokenProvider.generateAccessToken(
                attacker.getId(), List.of("ROLE_RECEIVER"));

        mockMvc.perform(get("/api/v1/blood-requests/" + bloodRequest.getId() + "/matches")
                        .header("Authorization", "Bearer " + attackerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Security: Suspended user attempting to query matching donors must return 401 Unauthorized")
    void testGetMatches_SuspendedUser_Returns401() throws Exception {
        String suspendedToken = jwtTokenProvider.generateAccessToken(
                suspendedUser.getId(), List.of("ROLE_RECEIVER"));

        mockMvc.perform(get("/api/v1/blood-requests/" + bloodRequest.getId() + "/matches")
                        .header("Authorization", "Bearer " + suspendedToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security: Request owner is authorized to query matches")
    void testGetMatches_Owner_Returns200() throws Exception {
        String ownerToken = jwtTokenProvider.generateAccessToken(
                requester.getId(), List.of("ROLE_RECEIVER"));

        mockMvc.perform(get("/api/v1/blood-requests/" + bloodRequest.getId() + "/matches")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(bloodRequest.getId().toString()))
                .andExpect(jsonPath("$.bloodGroupRequired").value("O+"))
                .andExpect(jsonPath("$.disclaimer").isNotEmpty());
    }

    @Test
    @DisplayName("Security: Admin is authorized to query matches for any blood request")
    void testGetMatches_Admin_Returns200() throws Exception {
        String adminToken = jwtTokenProvider.generateAccessToken(
                admin.getId(), List.of("ROLE_ADMIN"));

        mockMvc.perform(get("/api/v1/blood-requests/" + bloodRequest.getId() + "/matches")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(bloodRequest.getId().toString()));
    }

    @Test
    @DisplayName("Security Rate Limiting: 30 requests/min succeed, 31st returns 429 Too Many Requests")
    void testGetMatches_RateLimiting_Enforces30PerMinute() throws Exception {
        String ownerToken = jwtTokenProvider.generateAccessToken(
                requester.getId(), List.of("ROLE_RECEIVER"));

        // 30 requests should succeed
        for (int i = 1; i <= 30; i++) {
            mockMvc.perform(get("/api/v1/blood-requests/" + bloodRequest.getId() + "/matches")
                            .header("Authorization", "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        // 31st request should be rejected by matching rate limiter
        mockMvc.perform(get("/api/v1/blood-requests/" + bloodRequest.getId() + "/matches")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.message", containsString("Too many matching requests")));
    }

    @Test
    @DisplayName("Privacy & PII Audit: Response JSON NEVER leaks donor phone, email, full name, or raw coordinates")
    void testGetMatches_PrivacyAudit_ZeroPIILeakage() throws Exception {
        // Create candidate donor with private information
        String secretPhone = "+919999888877";
        String secretEmail = "confidential.donor.secrets@netra.org";
        String fullLegalName = "Jonathan Alexander Doe";
        double secretLat = 19.0800;
        double secretLng = 72.8800;

        User donorUser = new User(
                fullLegalName,
                secretEmail,
                secretPhone,
                passwordEncoder.encode("DonorSecretPass123!"),
                Set.of(UserRole.ROLE_DONOR)
        );
        donorUser.setStatus(UserStatus.ACTIVE);
        donorUser = userRepository.save(donorUser);

        DonorProfile donorProfile = new DonorProfile(donorUser.getId(), BloodGroup.O_POSITIVE, DonorAvailabilityStatus.AVAILABLE);
        donorProfile.setBloodGroupVerificationStatus(BloodGroupVerificationStatus.VERIFIED);
        donorProfile.setDonorStatus(DonorStatus.ACTIVE);
        donorProfile.setLatitude(secretLat);
        donorProfile.setLongitude(secretLng);
        donorProfileRepository.save(donorProfile);

        String ownerToken = jwtTokenProvider.generateAccessToken(
                requester.getId(), List.of("ROLE_RECEIVER"));

        MvcResult result = mockMvc.perform(get("/api/v1/blood-requests/" + bloodRequest.getId() + "/matches")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateCount").value(1))
                .andExpect(jsonPath("$.matches[0].donorDisplayName").value("Jonathan D."))
                .andExpect(jsonPath("$.matches[0].distanceKm").isNumber())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        // Strict negative assertions: verify sensitive private fields are absent from response body
        assertFalse(responseBody.contains(secretPhone), "Response must NOT leak donor phone number");
        assertFalse(responseBody.contains(secretEmail), "Response must NOT leak donor email");
        assertFalse(responseBody.contains(fullLegalName), "Response must NOT leak full legal name");
        assertFalse(responseBody.contains(String.valueOf(secretLat)), "Response must NOT leak raw donor latitude");
        assertFalse(responseBody.contains(String.valueOf(secretLng)), "Response must NOT leak raw donor longitude");
        assertFalse(responseBody.toLowerCase().contains("password"), "Response must NOT leak passwords or hashes");
        assertFalse(responseBody.toLowerCase().contains("phone"), "Response must NOT have phone key");
        assertFalse(responseBody.toLowerCase().contains("email"), "Response must NOT have email key");

        assertFalse(responseBody.contains("safeCandidateId"), "Response must NOT leak safeCandidateId to prevent cross-request tracking");

        // Positive check: masked name, match quality, and disclaimer exist
        assertTrue(responseBody.contains("Jonathan D."));
        assertTrue(responseBody.contains("matchQuality"));
        assertTrue(responseBody.contains("disclaimer"));
    }
}
