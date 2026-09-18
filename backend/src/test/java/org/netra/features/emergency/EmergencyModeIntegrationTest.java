package org.netra.features.emergency;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.core.audit.SecurityAuditLog;
import org.netra.core.audit.SecurityAuditLogRepository;
import org.netra.core.ratelimit.RateLimitingService;
import org.netra.core.security.JwtTokenProvider;
import org.netra.features.bloodrequest.dto.CancelBloodRequestRequest;
import org.netra.features.bloodrequest.dto.CreateBloodRequestRequest;
import org.netra.features.bloodrequest.entity.BloodRequest;
import org.netra.features.bloodrequest.entity.BloodRequestStatus;
import org.netra.features.bloodrequest.entity.BloodRequestUrgency;
import org.netra.features.bloodrequest.repository.BloodRequestRepository;
import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.emergency.dto.EmergencyBloodRequestRequest;
import org.netra.features.emergency.service.EmergencyIdempotencyService;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EmergencyModeIntegrationTest {

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
    private SecurityAuditLogRepository securityAuditLogRepository;

    @Autowired
    private RateLimitingService rateLimitingService;

    @Autowired
    private EmergencyIdempotencyService idempotencyService;

    private User requester;
    private User otherUser;
    private User admin;

    @BeforeEach
    void setUp() {
        securityAuditLogRepository.deleteAll();
        bloodRequestRepository.deleteAll();
        rateLimitingService.reset();
        idempotencyService.clear();

        requester = createTestUser("emergency.req", UserRole.ROLE_RECEIVER);
        otherUser = createTestUser("other.user", UserRole.ROLE_RECEIVER);
        admin = createTestUser("emergency.admin", UserRole.ROLE_ADMIN);
    }

    private User createTestUser(String prefix, UserRole role) {
        String email = prefix + "." + UUID.randomUUID() + "@netra.org";
        User user = new User(
                prefix + " User",
                email,
                "+919876543210",
                passwordEncoder.encode("SecurePass123"),
                Set.of(role)
        );
        return userRepository.save(user);
    }

    private EmergencyBloodRequestRequest buildValidEmergencyRequest() {
        return new EmergencyBloodRequestRequest(
                BloodGroup.O_POSITIVE,
                3,
                "Lilavati Hospital",
                "A-791, Bandra Reclamation, Bandra West",
                "Mumbai",
                "Maharashtra",
                "400050",
                19.0522,
                72.8295,
                Instant.now().plus(6, ChronoUnit.HOURS),
                "Urgent requirement for emergency surgery."
        );
    }

    @Test
    @DisplayName("Create emergency request forces urgency=CRITICAL, status=OPEN, and derives requesterUserId")
    void testCreateEmergencyRequest_Success() throws Exception {
        EmergencyBloodRequestRequest request = buildValidEmergencyRequest();
        String token = jwtTokenProvider.generateAccessToken(requester.getId(), List.of("ROLE_RECEIVER"));

        MvcResult result = mockMvc.perform(post("/api/v1/emergency/blood-requests")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "key-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.bloodGroup").value("O+"))
                .andExpect(jsonPath("$.unitsRequired").value(3))
                .andExpect(jsonPath("$.urgency").value("CRITICAL"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.requesterUserId").value(requester.getId().toString()))
                .andExpect(jsonPath("$.hospitalName").value("Lilavati Hospital"))
                .andExpect(jsonPath("$.city").value("Mumbai"))
                .andExpect(jsonPath("$.latitude").value(19.0522))
                .andExpect(jsonPath("$.longitude").value(72.8295))
                .andExpect(jsonPath("$.isOwner").value(true))
                .andExpect(jsonPath("$.canManage").value(true))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID requestId = UUID.fromString(json.get("id").asText());

        // Verify entity persisted directly in existing blood_requests table
        BloodRequest entity = bloodRequestRepository.findById(requestId).orElseThrow();
        assertEquals(requester.getId(), entity.getRequesterUserId());
        assertEquals(BloodRequestUrgency.CRITICAL, entity.getUrgency());
        assertEquals(BloodRequestStatus.OPEN, entity.getStatus());
        assertEquals("Lilavati Hospital", entity.getHospitalName());
    }

    @Test
    @DisplayName("Emergency deadline validation: must be future and within 72 hours")
    void testCreateEmergencyRequest_DeadlineValidation() throws Exception {
        String token = jwtTokenProvider.generateAccessToken(requester.getId(), List.of("ROLE_RECEIVER"));

        // Case 1: Deadline > 72 hours
        EmergencyBloodRequestRequest farFutureReq = buildValidEmergencyRequest();
        farFutureReq.setRequiredBy(Instant.now().plus(73, ChronoUnit.HOURS));

        mockMvc.perform(post("/api/v1/emergency/blood-requests")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "key-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(farFutureReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("within 72 hours")));

        // Case 2: Past deadline
        EmergencyBloodRequestRequest pastReq = buildValidEmergencyRequest();
        pastReq.setRequiredBy(Instant.now().minus(1, ChronoUnit.HOURS));

        mockMvc.perform(post("/api/v1/emergency/blood-requests")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "key-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pastReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("future")));

        // Case 3: Exactly valid (e.g. 71 hours)
        EmergencyBloodRequestRequest validReq = buildValidEmergencyRequest();
        validReq.setRequiredBy(Instant.now().plus(71, ChronoUnit.HOURS));

        mockMvc.perform(post("/api/v1/emergency/blood-requests")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "key-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Idempotency: Replaying with same Idempotency-Key returns 200 OK without duplicate DB insert")
    void testCreateEmergencyRequest_Idempotency() throws Exception {
        String token = jwtTokenProvider.generateAccessToken(requester.getId(), List.of("ROLE_RECEIVER"));
        EmergencyBloodRequestRequest request = buildValidEmergencyRequest();
        String idempotencyKey = "key-" + UUID.randomUUID();

        // First call
        MvcResult firstResult = mockMvc.perform(post("/api/v1/emergency/blood-requests")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode firstJson = objectMapper.readTree(firstResult.getResponse().getContentAsString());
        String firstId = firstJson.get("id").asText();

        // Duplicate call with same Idempotency-Key -> 200 OK replay
        MvcResult secondResult = mockMvc.perform(post("/api/v1/emergency/blood-requests")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode secondJson = objectMapper.readTree(secondResult.getResponse().getContentAsString());
        String secondId = secondJson.get("id").asText();

        assertEquals(firstId, secondId, "Idempotent calls must return the same request ID");
        assertEquals(1, bloodRequestRepository.count(), "Only one record should be in database");
    }

    @Test
    @DisplayName("Audit privacy: EMERGENCY_REQUEST_CREATED metadata contains strictly requestId and NO health/location data")
    void testAuditPrivacy_EmergencyCreated() throws Exception {
        String token = jwtTokenProvider.generateAccessToken(requester.getId(), List.of("ROLE_RECEIVER"));
        EmergencyBloodRequestRequest request = buildValidEmergencyRequest();

        MvcResult result = mockMvc.perform(post("/api/v1/emergency/blood-requests")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "key-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String requestId = json.get("id").asText();

        List<SecurityAuditLog> logs = securityAuditLogRepository.findAll().stream()
                .filter(l -> "EMERGENCY_REQUEST_CREATED".equals(l.getEventType()))
                .toList();

        assertFalse(logs.isEmpty(), "EMERGENCY_REQUEST_CREATED audit event must be logged");
        SecurityAuditLog log = logs.get(logs.size() - 1);
        assertEquals(requester.getId(), log.getUserId());

        String metadata = log.getMetadata();
        assertNotNull(metadata);
        assertTrue(metadata.contains("\"requestId\":\"" + requestId + "\""), "Must include requestId");

        // Verify privacy: no coordinates, blood group, description, or patient data
        assertFalse(metadata.contains("bloodGroup"), "Audit log must not contain bloodGroup");
        assertFalse(metadata.contains("O_POSITIVE"), "Audit log must not contain blood group literal");
        assertFalse(metadata.contains("19.0522"), "Audit log must not contain latitude");
        assertFalse(metadata.contains("72.8295"), "Audit log must not contain longitude");
        assertFalse(metadata.contains("Lilavati"), "Audit log must not contain hospital name");
        assertFalse(metadata.contains("surgery"), "Audit log must not contain description");
    }

    @Test
    @DisplayName("Cancel emergency request by owner logs EMERGENCY_REQUEST_CANCELLED")
    void testCancelEmergencyRequest_Success() throws Exception {
        String token = jwtTokenProvider.generateAccessToken(requester.getId(), List.of("ROLE_RECEIVER"));
        EmergencyBloodRequestRequest request = buildValidEmergencyRequest();

        MvcResult createResult = mockMvc.perform(post("/api/v1/emergency/blood-requests")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "key-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String requestId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        CancelBloodRequestRequest cancelReq = new CancelBloodRequestRequest("Emergency resolved with hospital stock.");

        mockMvc.perform(post("/api/v1/emergency/blood-requests/" + requestId + "/cancel")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("Emergency resolved with hospital stock."))
                .andExpect(jsonPath("$.isOwner").value(true))
                .andExpect(jsonPath("$.canManage").value(true));

        // Verify entity state
        BloodRequest entity = bloodRequestRepository.findById(UUID.fromString(requestId)).orElseThrow();
        assertEquals(BloodRequestStatus.CANCELLED, entity.getStatus());
        assertNotNull(entity.getCancelledAt());
        assertEquals(requester.getId(), entity.getCancelledBy());

        // Verify audit log
        List<SecurityAuditLog> logs = securityAuditLogRepository.findAll().stream()
                .filter(l -> "EMERGENCY_REQUEST_CANCELLED".equals(l.getEventType()))
                .toList();
        assertFalse(logs.isEmpty());
        SecurityAuditLog cancelLog = logs.get(logs.size() - 1);
        assertEquals(requester.getId(), cancelLog.getUserId());
        assertTrue(cancelLog.getMetadata().contains("\"requestId\":\"" + requestId + "\""));
        assertFalse(cancelLog.getMetadata().contains("hospital stock"));
    }

    @Test
    @DisplayName("Emergency requests are discoverable via existing blood request discovery and nearby APIs")
    void testDiscoveryCompatibility() throws Exception {
        String token = jwtTokenProvider.generateAccessToken(requester.getId(), List.of("ROLE_RECEIVER"));
        EmergencyBloodRequestRequest request = buildValidEmergencyRequest();

        MvcResult createResult = mockMvc.perform(post("/api/v1/emergency/blood-requests")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "key-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String requestId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // Discoverable via public GET /api/v1/blood-requests
        mockMvc.perform(get("/api/v1/blood-requests")
                        .param("urgency", "CRITICAL")
                        .param("city", "Mumbai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(requestId))
                .andExpect(jsonPath("$.content[0].urgency").value("CRITICAL"));

        // Discoverable via public GET /api/v1/blood-requests/nearby
        mockMvc.perform(get("/api/v1/blood-requests/nearby")
                        .param("latitude", "19.0520")
                        .param("longitude", "72.8290")
                        .param("radiusKm", "10.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(requestId))
                .andExpect(jsonPath("$[0].urgency").value("CRITICAL"));

        // Public detail masking for unauthenticated caller
        mockMvc.perform(get("/api/v1/blood-requests/" + requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requestId))
                .andExpect(jsonPath("$.bloodGroup").value("O+"))
                .andExpect(jsonPath("$.hospitalName").value("Lilavati Hospital"))
                .andExpect(jsonPath("$.isOwner").value(false))
                .andExpect(jsonPath("$.canManage").value(false))
                .andExpect(jsonPath("$.requesterUserId").doesNotExist())
                .andExpect(jsonPath("$.latitude").doesNotExist())
                .andExpect(jsonPath("$.longitude").doesNotExist())
                .andExpect(jsonPath("$.description").doesNotExist());
    }

    @Test
    @DisplayName("Admin cancels CRITICAL emergency request -> success with EMERGENCY_REQUEST_CANCELLED audit")
    void testCancelEmergencyRequest_ByAdmin_Success() throws Exception {
        String userToken = jwtTokenProvider.generateAccessToken(requester.getId(), List.of("ROLE_RECEIVER"));
        String adminToken = jwtTokenProvider.generateAccessToken(admin.getId(), List.of("ROLE_ADMIN"));
        EmergencyBloodRequestRequest request = buildValidEmergencyRequest();

        MvcResult createResult = mockMvc.perform(post("/api/v1/emergency/blood-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .header("Idempotency-Key", "key-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String requestId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();
        CancelBloodRequestRequest cancelReq = new CancelBloodRequestRequest("Admin cancelled for verification.");

        mockMvc.perform(post("/api/v1/emergency/blood-requests/" + requestId + "/cancel")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.isOwner").value(false))
                .andExpect(jsonPath("$.canManage").value(true));

        // Verify EMERGENCY_REQUEST_CANCELLED logged with admin as actor
        List<SecurityAuditLog> logs = securityAuditLogRepository.findAll().stream()
                .filter(l -> "EMERGENCY_REQUEST_CANCELLED".equals(l.getEventType()))
                .toList();
        assertFalse(logs.isEmpty());
        SecurityAuditLog log = logs.get(logs.size() - 1);
        assertEquals(admin.getId(), log.getUserId());
    }

    @Test
    @DisplayName("Owner attempts emergency cancellation against NORMAL blood request -> rejected 400 VALIDATION_ERROR")
    void testCancelEmergencyRequest_AgainstNormalRequest_Rejected() throws Exception {
        String token = jwtTokenProvider.generateAccessToken(requester.getId(), List.of("ROLE_RECEIVER"));

        // Create normal request directly in database with NORMAL urgency
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

        CancelBloodRequestRequest cancelReq = new CancelBloodRequestRequest("Attempting emergency cancellation on normal request.");

        mockMvc.perform(post("/api/v1/emergency/blood-requests/" + normalReq.getId() + "/cancel")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("Only emergency requests with CRITICAL urgency can be cancelled")));

        // Verify no EMERGENCY_REQUEST_CANCELLED logged
        long emergencyCancelLogs = securityAuditLogRepository.findAll().stream()
                .filter(l -> "EMERGENCY_REQUEST_CANCELLED".equals(l.getEventType()))
                .count();
        assertEquals(0, emergencyCancelLogs);
    }

    @Test
    @DisplayName("Owner attempts emergency cancellation against URGENT blood request -> rejected 400 VALIDATION_ERROR")
    void testCancelEmergencyRequest_AgainstUrgentRequest_Rejected() throws Exception {
        String token = jwtTokenProvider.generateAccessToken(requester.getId(), List.of("ROLE_RECEIVER"));

        // Create URGENT request directly in database
        BloodRequest urgentReq = new BloodRequest();
        urgentReq.setRequesterUserId(requester.getId());
        urgentReq.setBloodGroup(BloodGroup.B_POSITIVE);
        urgentReq.setUnitsRequired(2);
        urgentReq.setUrgency(BloodRequestUrgency.URGENT);
        urgentReq.setStatus(BloodRequestStatus.OPEN);
        urgentReq.setHospitalName("Hinduja Hospital");
        urgentReq.setHospitalAddress("Veer Savarkar Marg, Mahim");
        urgentReq.setCity("Mumbai");
        urgentReq.setState("Maharashtra");
        urgentReq.setPostalCode("400016");
        urgentReq.setLatitude(19.0330);
        urgentReq.setLongitude(72.8397);
        urgentReq.setRequiredBy(Instant.now().plus(24, ChronoUnit.HOURS));
        urgentReq.setCreatedAt(Instant.now());
        urgentReq.setUpdatedAt(Instant.now());
        urgentReq = bloodRequestRepository.save(urgentReq);

        CancelBloodRequestRequest cancelReq = new CancelBloodRequestRequest("Attempting emergency cancellation on urgent request.");

        mockMvc.perform(post("/api/v1/emergency/blood-requests/" + urgentReq.getId() + "/cancel")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("Only emergency requests with CRITICAL urgency can be cancelled")));

        // Verify no EMERGENCY_REQUEST_CANCELLED logged
        long emergencyCancelLogs = securityAuditLogRepository.findAll().stream()
                .filter(l -> "EMERGENCY_REQUEST_CANCELLED".equals(l.getEventType()))
                .count();
        assertEquals(0, emergencyCancelLogs);
    }

    @Test
    @DisplayName("Normal Blood Request cancellation works through normal endpoint and emits BLOOD_REQUEST_CANCELLED only")
    void testNormalBloodRequestCancellation_EmitsBloodRequestCancelledOnly() throws Exception {
        String token = jwtTokenProvider.generateAccessToken(requester.getId(), List.of("ROLE_RECEIVER"));

        // Create normal request
        BloodRequest normalReq = new BloodRequest();
        normalReq.setRequesterUserId(requester.getId());
        normalReq.setBloodGroup(BloodGroup.A_POSITIVE);
        normalReq.setUnitsRequired(1);
        normalReq.setUrgency(BloodRequestUrgency.NORMAL);
        normalReq.setStatus(BloodRequestStatus.OPEN);
        normalReq.setHospitalName("Nanavati Hospital");
        normalReq.setHospitalAddress("SV Road, Vile Parle West");
        normalReq.setCity("Mumbai");
        normalReq.setState("Maharashtra");
        normalReq.setPostalCode("400056");
        normalReq.setLatitude(19.0968);
        normalReq.setLongitude(72.8428);
        normalReq.setRequiredBy(Instant.now().plus(48, ChronoUnit.HOURS));
        normalReq.setCreatedAt(Instant.now());
        normalReq.setUpdatedAt(Instant.now());
        normalReq = bloodRequestRepository.save(normalReq);

        CancelBloodRequestRequest cancelReq = new CancelBloodRequestRequest("Cancelled through normal endpoint.");

        // Cancel via normal endpoint /api/v1/blood-requests/{id}/cancel
        mockMvc.perform(post("/api/v1/blood-requests/" + normalReq.getId() + "/cancel")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Verify BLOOD_REQUEST_CANCELLED is present
        long normalCancelCount = securityAuditLogRepository.findAll().stream()
                .filter(l -> "BLOOD_REQUEST_CANCELLED".equals(l.getEventType()))
                .count();
        assertTrue(normalCancelCount > 0, "BLOOD_REQUEST_CANCELLED must be emitted");

        // Verify ZERO EMERGENCY_REQUEST_CANCELLED audit events emitted
        long emergencyCancelCount = securityAuditLogRepository.findAll().stream()
                .filter(l -> "EMERGENCY_REQUEST_CANCELLED".equals(l.getEventType()))
                .count();
        assertEquals(0, emergencyCancelCount, "EMERGENCY_REQUEST_CANCELLED must NEVER be emitted for normal blood requests");
    }

    @Test
    @DisplayName("Normal Blood Request creation path rejects urgency=CRITICAL (CRITICAL must use Emergency Mode)")
    void testNormalBloodRequestCreation_CriticalUrgency_Rejected() throws Exception {
        String token = jwtTokenProvider.generateAccessToken(requester.getId(), List.of("ROLE_RECEIVER"));

        CreateBloodRequestRequest normalRequest = new CreateBloodRequestRequest();
        normalRequest.setBloodGroup(BloodGroup.O_NEGATIVE);
        normalRequest.setUnitsRequired(2);
        normalRequest.setUrgency(BloodRequestUrgency.CRITICAL); // Attempting CRITICAL through normal endpoint
        normalRequest.setHospitalName("Jaslok Hospital");
        normalRequest.setHospitalAddress("15 Dr. G. Deshmukh Marg");
        normalRequest.setCity("Mumbai");
        normalRequest.setState("Maharashtra");
        normalRequest.setPostalCode("400026");
        normalRequest.setLatitude(18.9715);
        normalRequest.setLongitude(72.8098);
        normalRequest.setRequiredBy(Instant.now().plus(12, ChronoUnit.HOURS));

        mockMvc.perform(post("/api/v1/blood-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(normalRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("Critical urgency requests must be created through Emergency Mode")));
    }
}
