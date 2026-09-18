package org.netra.features.emergency;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.core.ratelimit.RateLimitingService;
import org.netra.core.security.JwtTokenProvider;
import org.netra.features.bloodrequest.dto.CancelBloodRequestRequest;
import org.netra.features.bloodrequest.entity.BloodRequest;
import org.netra.features.bloodrequest.entity.BloodRequestStatus;
import org.netra.features.bloodrequest.entity.BloodRequestUrgency;
import org.netra.features.bloodrequest.repository.BloodRequestRepository;
import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.emergency.dto.EmergencyBloodRequestRequest;
import org.netra.features.emergency.service.EmergencyIdempotencyService;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EmergencyModeSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RateLimitingService rateLimitingService;

    @Autowired
    private EmergencyIdempotencyService idempotencyService;

    private User requester;
    private User attacker;
    private User suspendedUser;

    @BeforeEach
    void setUp() {
        bloodRequestRepository.deleteAll();
        rateLimitingService.reset();
        idempotencyService.clear();

        requester = createTestUser("req.sec", UserRole.ROLE_RECEIVER, UserStatus.ACTIVE);
        attacker = createTestUser("attacker.sec", UserRole.ROLE_RECEIVER, UserStatus.ACTIVE);
        suspendedUser = createTestUser("susp.sec", UserRole.ROLE_RECEIVER, UserStatus.SUSPENDED);
    }

    private User createTestUser(String prefix, UserRole role, UserStatus status) {
        String email = prefix + "." + UUID.randomUUID() + "@netra.org";
        User user = new User(
                prefix + " User",
                email,
                "+919876543210",
                passwordEncoder.encode("SecurePass123"),
                Set.of(role)
        );
        user.setStatus(status);
        return userRepository.save(user);
    }

    private EmergencyBloodRequestRequest buildValidEmergencyRequest() {
        return new EmergencyBloodRequestRequest(
                BloodGroup.A_POSITIVE,
                2,
                "KEM Hospital",
                "Acharya Donde Marg, Parel",
                "Mumbai",
                "Maharashtra",
                "400012",
                19.0028,
                72.8423,
                Instant.now().plus(4, ChronoUnit.HOURS),
                "Critical trauma patient."
        );
    }

    @Test
    @DisplayName("Anonymous creation request must be rejected with 401 Unauthorized")
    void testCreateEmergencyRequest_Anonymous_Rejected() throws Exception {
        EmergencyBloodRequestRequest request = buildValidEmergencyRequest();

        mockMvc.perform(post("/api/v1/emergency/blood-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Suspended user creation request must be rejected with 403 Forbidden")
    void testCreateEmergencyRequest_SuspendedUser_Rejected() throws Exception {
        EmergencyBloodRequestRequest request = buildValidEmergencyRequest();
        String token = jwtTokenProvider.generateAccessToken(
                suspendedUser.getId(), List.of("ROLE_RECEIVER"));

        mockMvc.perform(post("/api/v1/emergency/blood-requests")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "key-susp-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Rate limiting: 5 emergency creations allowed per 10 minutes, 6th returns 429 RATE_LIMIT_EXCEEDED")
    void testCreateEmergencyRequest_RateLimiting() throws Exception {
        String token = jwtTokenProvider.generateAccessToken(
                requester.getId(), List.of("ROLE_RECEIVER"));

        // 5 valid requests with distinct idempotency keys
        for (int i = 1; i <= 5; i++) {
            EmergencyBloodRequestRequest req = buildValidEmergencyRequest();
            req.setHospitalName("Hospital " + i);
            mockMvc.perform(post("/api/v1/emergency/blood-requests")
                            .header("Authorization", "Bearer " + token)
                            .header("Idempotency-Key", "req-key-" + i)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());
        }

        // 6th distinct request must be rate-limited
        EmergencyBloodRequestRequest req6 = buildValidEmergencyRequest();
        req6.setHospitalName("Hospital 6");
        mockMvc.perform(post("/api/v1/emergency/blood-requests")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "req-key-6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req6)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.message", containsString("Too many emergency requests")));
    }

    @Test
    @DisplayName("BOLA / IDOR: Non-owner non-admin user cannot cancel another user's CRITICAL emergency request -> 403 Forbidden")
    void testCancelEmergencyRequest_BOLA_Protection() throws Exception {
        // Create emergency request as requester
        BloodRequest bloodRequest = new BloodRequest();
        bloodRequest.setRequesterUserId(requester.getId());
        bloodRequest.setBloodGroup(BloodGroup.B_POSITIVE);
        bloodRequest.setUnitsRequired(2);
        bloodRequest.setUrgency(BloodRequestUrgency.CRITICAL);
        bloodRequest.setStatus(BloodRequestStatus.OPEN);
        bloodRequest.setHospitalName("Hinduja Hospital");
        bloodRequest.setHospitalAddress("Mahim");
        bloodRequest.setCity("Mumbai");
        bloodRequest.setState("Maharashtra");
        bloodRequest.setPostalCode("400016");
        bloodRequest.setLatitude(19.0330);
        bloodRequest.setLongitude(72.8397);
        bloodRequest.setRequiredBy(Instant.now().plus(6, ChronoUnit.HOURS));
        bloodRequest.setCreatedAt(Instant.now());
        bloodRequest.setUpdatedAt(Instant.now());
        BloodRequest saved = bloodRequestRepository.save(bloodRequest);

        // Attacker attempts to cancel requester's request
        String attackerToken = jwtTokenProvider.generateAccessToken(
                attacker.getId(), List.of("ROLE_RECEIVER"));

        CancelBloodRequestRequest cancelReq = new CancelBloodRequestRequest("Malicious cancel attempt");

        mockMvc.perform(post("/api/v1/emergency/blood-requests/" + saved.getId() + "/cancel")
                        .header("Authorization", "Bearer " + attackerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Information Disclosure Protection: Attacker cancelling another user's NORMAL request returns 403 Forbidden (NOT 400)")
    void testCancelEmergencyRequest_AttackerAgainstNormalRequest_ReturnsForbidden() throws Exception {
        BloodRequest normalReq = new BloodRequest();
        normalReq.setRequesterUserId(requester.getId());
        normalReq.setBloodGroup(BloodGroup.O_POSITIVE);
        normalReq.setUnitsRequired(2);
        normalReq.setUrgency(BloodRequestUrgency.NORMAL);
        normalReq.setStatus(BloodRequestStatus.OPEN);
        normalReq.setHospitalName("KEM Hospital");
        normalReq.setHospitalAddress("Acharya Donde Marg, Parel");
        normalReq.setCity("Mumbai");
        normalReq.setState("Maharashtra");
        normalReq.setPostalCode("400012");
        normalReq.setLatitude(19.0028);
        normalReq.setLongitude(72.8423);
        normalReq.setRequiredBy(Instant.now().plus(24, ChronoUnit.HOURS));
        normalReq.setCreatedAt(Instant.now());
        normalReq.setUpdatedAt(Instant.now());
        normalReq = bloodRequestRepository.save(normalReq);

        String attackerToken = jwtTokenProvider.generateAccessToken(
                attacker.getId(), List.of("ROLE_RECEIVER"));

        CancelBloodRequestRequest cancelReq = new CancelBloodRequestRequest("Malicious probing");

        mockMvc.perform(post("/api/v1/emergency/blood-requests/" + normalReq.getId() + "/cancel")
                        .header("Authorization", "Bearer " + attackerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Information Disclosure Protection: Attacker cancelling another user's URGENT request returns 403 Forbidden (NOT 400)")
    void testCancelEmergencyRequest_AttackerAgainstUrgentRequest_ReturnsForbidden() throws Exception {
        BloodRequest urgentReq = new BloodRequest();
        urgentReq.setRequesterUserId(requester.getId());
        urgentReq.setBloodGroup(BloodGroup.A_POSITIVE);
        urgentReq.setUnitsRequired(1);
        urgentReq.setUrgency(BloodRequestUrgency.URGENT);
        urgentReq.setStatus(BloodRequestStatus.OPEN);
        urgentReq.setHospitalName("Nanavati Hospital");
        urgentReq.setHospitalAddress("SV Road, Vile Parle West");
        urgentReq.setCity("Mumbai");
        urgentReq.setState("Maharashtra");
        urgentReq.setPostalCode("400056");
        urgentReq.setLatitude(19.0968);
        urgentReq.setLongitude(72.8428);
        urgentReq.setRequiredBy(Instant.now().plus(12, ChronoUnit.HOURS));
        urgentReq.setCreatedAt(Instant.now());
        urgentReq.setUpdatedAt(Instant.now());
        urgentReq = bloodRequestRepository.save(urgentReq);

        String attackerToken = jwtTokenProvider.generateAccessToken(
                attacker.getId(), List.of("ROLE_RECEIVER"));

        CancelBloodRequestRequest cancelReq = new CancelBloodRequestRequest("Malicious probing");

        mockMvc.perform(post("/api/v1/emergency/blood-requests/" + urgentReq.getId() + "/cancel")
                        .header("Authorization", "Bearer " + attackerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Security: Client cannot override urgency, status, or inject requesterUserId")
    void testCreateEmergencyRequest_CannotOverrideUrgencyOrStatus() throws Exception {
        String token = jwtTokenProvider.generateAccessToken(
                requester.getId(), List.of("ROLE_RECEIVER"));

        String maliciousJson = """
                {
                    "bloodGroup": "B+",
                    "unitsRequired": 2,
                    "urgency": "NORMAL",
                    "status": "FULFILLED",
                    "requesterUserId": "00000000-0000-0000-0000-000000000000",
                    "hospitalName": "Tata Memorial",
                    "hospitalAddress": "Dr E Borges Road, Parel",
                    "city": "Mumbai",
                    "state": "Maharashtra",
                    "postalCode": "400012",
                    "latitude": 19.0048,
                    "longitude": 72.8428,
                    "requiredBy": "%s",
                    "description": "Attempting parameter tampering"
                }
                """.formatted(Instant.now().plus(6, ChronoUnit.HOURS));

        mockMvc.perform(post("/api/v1/emergency/blood-requests")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "tamper-key-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(maliciousJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.urgency").value("CRITICAL"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.requesterUserId").value(requester.getId().toString()));
    }
}
