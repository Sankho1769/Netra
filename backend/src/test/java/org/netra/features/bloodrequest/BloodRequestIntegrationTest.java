package org.netra.features.bloodrequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.core.audit.SecurityAuditLog;
import org.netra.core.audit.SecurityAuditLogRepository;
import org.netra.core.exception.AccountStatusException;
import org.netra.core.security.JwtTokenProvider;
import org.netra.features.bloodrequest.dto.*;
import org.netra.features.bloodrequest.entity.BloodRequest;
import org.netra.features.bloodrequest.entity.BloodRequestStatus;
import org.netra.features.bloodrequest.entity.BloodRequestUrgency;
import org.netra.features.bloodrequest.repository.BloodRequestRepository;
import org.netra.features.bloodrequest.service.BloodRequestAuthorizationService;
import org.netra.features.bloodrequest.service.BloodRequestExpirationService;
import org.netra.features.donor.entity.BloodGroup;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BloodRequestIntegrationTest {

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
    private BloodRequestExpirationService expirationService;

    @Autowired
    private BloodRequestAuthorizationService authorizationService;

    @Autowired
    private SecurityAuditLogRepository securityAuditLogRepository;

    private User user1;
    private User user2;
    private User adminUser;
    private User suspendedUser;

    @BeforeEach
    void setUp() {
        bloodRequestRepository.deleteAll();

        user1 = createTestUser("requester1", UserRole.ROLE_RECEIVER);
        user2 = createTestUser("requester2", UserRole.ROLE_RECEIVER);
        adminUser = createTestUser("admin", UserRole.ROLE_ADMIN);

        suspendedUser = createTestUser("suspended", UserRole.ROLE_RECEIVER);
        suspendedUser.setStatus(UserStatus.SUSPENDED);
        userRepository.save(suspendedUser);
    }

    private User createTestUser(String prefix, UserRole... roles) {
        String email = prefix.toLowerCase() + "." + UUID.randomUUID() + "@netra.org";
        User user = new User(
                prefix + " User",
                email,
                "+919876543210",
                passwordEncoder.encode("SecurePass123"),
                Set.of(roles)
        );
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private String getAccessToken(User user) {
        return jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getRoles().stream().map(Enum::name).toList()
        );
    }

    private CreateBloodRequestRequest createSampleCreateRequest() {
        CreateBloodRequestRequest req = new CreateBloodRequestRequest();
        req.setBloodGroup(BloodGroup.O_POSITIVE);
        req.setUnitsRequired(3);
        req.setUrgency(BloodRequestUrgency.URGENT);
        req.setHospitalName("Apollo Memorial Hospital");
        req.setHospitalAddress("123 Health Boulevard, Sector 4");
        req.setCity("Mumbai");
        req.setState("Maharashtra");
        req.setPostalCode("400001");
        req.setLatitude(18.9401);
        req.setLongitude(72.8347);
        req.setRequiredBy(Instant.now().plus(48, ChronoUnit.HOURS));
        req.setDescription("Emergency blood units needed for planned heart surgery.");
        return req;
    }

    @Test
    @DisplayName("Create Blood Request - Valid payload creates request with 201 and derives requester from SecurityContext; Audit metadata contains NO health data")
    void testCreateBloodRequest_Success() throws Exception {
        CreateBloodRequestRequest req = createSampleCreateRequest();

        MvcResult result = mockMvc.perform(post("/api/v1/blood-requests")
                        .header("Authorization", "Bearer " + getAccessToken(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.bloodGroup").value("O+"))
                .andExpect(jsonPath("$.unitsRequired").value(3))
                .andExpect(jsonPath("$.urgency").value("URGENT"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.hospitalName").value("Apollo Memorial Hospital"))
                .andExpect(jsonPath("$.city").value("Mumbai"))
                .andExpect(jsonPath("$.requesterUserId").value(user1.getId().toString()))
                .andExpect(jsonPath("$.isOwner").value(true))
                .andExpect(jsonPath("$.latitude").value(18.9401))
                .andExpect(jsonPath("$.longitude").value(72.8347))
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID requestId = UUID.fromString(responseNode.get("id").asText());

        BloodRequest persisted = bloodRequestRepository.findById(requestId).orElseThrow();
        assertEquals(user1.getId(), persisted.getRequesterUserId());
        assertEquals(BloodRequestStatus.OPEN, persisted.getStatus());
        assertEquals(0L, persisted.getVersion());

        // Verify audit log - strictly requestId only, NO health/location data in metadata
        List<SecurityAuditLog> logs = securityAuditLogRepository.findByUserIdOrderByCreatedAtDesc(user1.getId());
        SecurityAuditLog createdAudit = logs.stream()
                .filter(l -> "BLOOD_REQUEST_CREATED".equals(l.getEventType()))
                .findFirst().orElseThrow();
        assertEquals("{\"requestId\":\"" + requestId + "\"}", createdAudit.getMetadata());
        assertFalse(createdAudit.getMetadata().contains("bloodGroup"));
        assertFalse(createdAudit.getMetadata().contains("latitude"));
        assertFalse(createdAudit.getMetadata().contains("description"));
    }

    @Test
    @DisplayName("Create Blood Request - Unauthenticated access is rejected")
    void testCreateBloodRequest_Unauthenticated() throws Exception {
        CreateBloodRequestRequest req = createSampleCreateRequest();

        mockMvc.perform(post("/api/v1/blood-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Create Blood Request - Inactive / suspended user is rejected by JwtAuthenticationFilter and AuthorizationService")
    void testCreateBloodRequest_SuspendedUser() throws Exception {
        CreateBloodRequestRequest req = createSampleCreateRequest();

        // JwtAuthenticationFilter rejects suspended users with 401 Unauthorized
        mockMvc.perform(post("/api/v1/blood-requests")
                        .header("Authorization", "Bearer " + getAccessToken(suspendedUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());

        // Direct service call throws AccountStatusException
        assertThrows(AccountStatusException.class, () -> authorizationService.verifyActiveUser(suspendedUser.getId()));
    }

    @Test
    @DisplayName("Create Blood Request - Validation errors for invalid units, past requiredBy, invalid coordinates")
    void testCreateBloodRequest_ValidationFailures() throws Exception {
        // Zero units
        CreateBloodRequestRequest reqZeroUnits = createSampleCreateRequest();
        reqZeroUnits.setUnitsRequired(0);
        mockMvc.perform(post("/api/v1/blood-requests")
                        .header("Authorization", "Bearer " + getAccessToken(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqZeroUnits)))
                .andExpect(status().isBadRequest());

        // Exceeds 50 units
        CreateBloodRequestRequest reqExcessUnits = createSampleCreateRequest();
        reqExcessUnits.setUnitsRequired(51);
        mockMvc.perform(post("/api/v1/blood-requests")
                        .header("Authorization", "Bearer " + getAccessToken(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqExcessUnits)))
                .andExpect(status().isBadRequest());

        // Past deadline
        CreateBloodRequestRequest reqPastDate = createSampleCreateRequest();
        reqPastDate.setRequiredBy(Instant.now().minus(1, ChronoUnit.HOURS));
        mockMvc.perform(post("/api/v1/blood-requests")
                        .header("Authorization", "Bearer " + getAccessToken(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqPastDate)))
                .andExpect(status().isBadRequest());

        // Missing hospital name
        CreateBloodRequestRequest reqNoHospital = createSampleCreateRequest();
        reqNoHospital.setHospitalName("   ");
        mockMvc.perform(post("/api/v1/blood-requests")
                        .header("Authorization", "Bearer " + getAccessToken(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqNoHospital)))
                .andExpect(status().isBadRequest());

        // Invalid latitude
        CreateBloodRequestRequest reqBadLat = createSampleCreateRequest();
        reqBadLat.setLatitude(95.0);
        mockMvc.perform(post("/api/v1/blood-requests")
                        .header("Authorization", "Bearer " + getAccessToken(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBadLat)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Update Blood Request - Owner can update mutable operational fields (NORMAL -> URGENT)")
    void testUpdateBloodRequest_NormalToUrgent_Success() throws Exception {
        CreateBloodRequestRequest createReq = createSampleCreateRequest();
        createReq.setUrgency(BloodRequestUrgency.NORMAL);
        MvcResult createResult = mockMvc.perform(post("/api/v1/blood-requests")
                        .header("Authorization", "Bearer " + getAccessToken(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID requestId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());

        UpdateBloodRequestRequest updateReq = new UpdateBloodRequestRequest();
        updateReq.setUnitsRequired(5);
        updateReq.setUrgency(BloodRequestUrgency.URGENT);
        updateReq.setHospitalName("Apollo Multi-Specialty");

        mockMvc.perform(patch("/api/v1/blood-requests/" + requestId)
                        .header("Authorization", "Bearer " + getAccessToken(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitsRequired").value(5))
                .andExpect(jsonPath("$.urgency").value("URGENT"))
                .andExpect(jsonPath("$.hospitalName").value("Apollo Multi-Specialty"));

        BloodRequest updated = bloodRequestRepository.findById(requestId).orElseThrow();
        assertEquals(5, updated.getUnitsRequired());
        assertEquals(BloodRequestUrgency.URGENT, updated.getUrgency());
    }

    @Test
    @DisplayName("Update Blood Request - URGENT -> NORMAL succeeds")
    void testUpdateBloodRequest_UrgentToNormal_Success() throws Exception {
        CreateBloodRequestRequest createReq = createSampleCreateRequest();
        createReq.setUrgency(BloodRequestUrgency.URGENT);
        MvcResult createResult = mockMvc.perform(post("/api/v1/blood-requests")
                        .header("Authorization", "Bearer " + getAccessToken(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID requestId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());

        UpdateBloodRequestRequest updateReq = new UpdateBloodRequestRequest();
        updateReq.setUrgency(BloodRequestUrgency.NORMAL);

        mockMvc.perform(patch("/api/v1/blood-requests/" + requestId)
                        .header("Authorization", "Bearer " + getAccessToken(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urgency").value("NORMAL"));

        BloodRequest updated = bloodRequestRepository.findById(requestId).orElseThrow();
        assertEquals(BloodRequestUrgency.NORMAL, updated.getUrgency());
    }

    @Test
    @DisplayName("Update Blood Request - NORMAL -> CRITICAL returns 400 VALIDATION_ERROR")
    void testUpdateBloodRequest_NormalToCritical_Rejected() throws Exception {
        CreateBloodRequestRequest createReq = createSampleCreateRequest();
        createReq.setUrgency(BloodRequestUrgency.NORMAL);
        MvcResult createResult = mockMvc.perform(post("/api/v1/blood-requests")
                        .header("Authorization", "Bearer " + getAccessToken(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID requestId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());

        UpdateBloodRequestRequest updateReq = new UpdateBloodRequestRequest();
        updateReq.setUrgency(BloodRequestUrgency.CRITICAL);

        mockMvc.perform(patch("/api/v1/blood-requests/" + requestId)
                        .header("Authorization", "Bearer " + getAccessToken(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("Critical urgency requests must be created through Emergency Mode.")));

        BloodRequest unchanged = bloodRequestRepository.findById(requestId).orElseThrow();
        assertEquals(BloodRequestUrgency.NORMAL, unchanged.getUrgency());
    }

    @Test
    @DisplayName("Update Blood Request - URGENT -> CRITICAL returns 400 VALIDATION_ERROR")
    void testUpdateBloodRequest_UrgentToCritical_Rejected() throws Exception {
        CreateBloodRequestRequest createReq = createSampleCreateRequest();
        createReq.setUrgency(BloodRequestUrgency.URGENT);
        MvcResult createResult = mockMvc.perform(post("/api/v1/blood-requests")
                        .header("Authorization", "Bearer " + getAccessToken(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID requestId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());

        UpdateBloodRequestRequest updateReq = new UpdateBloodRequestRequest();
        updateReq.setUrgency(BloodRequestUrgency.CRITICAL);

        mockMvc.perform(patch("/api/v1/blood-requests/" + requestId)
                        .header("Authorization", "Bearer " + getAccessToken(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("Critical urgency requests must be created through Emergency Mode.")));

        BloodRequest unchanged = bloodRequestRepository.findById(requestId).orElseThrow();
        assertEquals(BloodRequestUrgency.URGENT, unchanged.getUrgency());
    }

    @Test
    @DisplayName("Update Blood Request - Existing CRITICAL emergency request remains CRITICAL")
    void testUpdateBloodRequest_CriticalRemainsCritical_Success() throws Exception {
        BloodRequest criticalReq = new BloodRequest();
        criticalReq.setRequesterUserId(user1.getId());
        criticalReq.setBloodGroup(BloodGroup.O_POSITIVE);
        criticalReq.setUnitsRequired(3);
        criticalReq.setUrgency(BloodRequestUrgency.CRITICAL);
        criticalReq.setStatus(BloodRequestStatus.OPEN);
        criticalReq.setHospitalName("Lilavati Hospital");
        criticalReq.setHospitalAddress("A-791, Bandra Reclamation");
        criticalReq.setCity("Mumbai");
        criticalReq.setState("Maharashtra");
        criticalReq.setPostalCode("400050");
        criticalReq.setLatitude(19.0522);
        criticalReq.setLongitude(72.8295);
        criticalReq.setRequiredBy(Instant.now().plus(6, ChronoUnit.HOURS));
        criticalReq.setCreatedAt(Instant.now());
        criticalReq.setUpdatedAt(Instant.now());
        criticalReq = bloodRequestRepository.save(criticalReq);

        UpdateBloodRequestRequest updateReq = new UpdateBloodRequestRequest();
        updateReq.setUrgency(BloodRequestUrgency.CRITICAL);
        updateReq.setHospitalName("Lilavati Hospital & Research Centre");

        mockMvc.perform(patch("/api/v1/blood-requests/" + criticalReq.getId())
                        .header("Authorization", "Bearer " + getAccessToken(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urgency").value("CRITICAL"))
                .andExpect(jsonPath("$.hospitalName").value("Lilavati Hospital & Research Centre"));

        BloodRequest updated = bloodRequestRepository.findById(criticalReq.getId()).orElseThrow();
        assertEquals(BloodRequestUrgency.CRITICAL, updated.getUrgency());
        assertEquals("Lilavati Hospital & Research Centre", updated.getHospitalName());
    }

    @Test
    @DisplayName("Update Blood Request - CRITICAL -> NORMAL returns 400 VALIDATION_ERROR")
    void testUpdateBloodRequest_CriticalToNormal_Rejected() throws Exception {
        BloodRequest criticalReq = new BloodRequest();
        criticalReq.setRequesterUserId(user1.getId());
        criticalReq.setBloodGroup(BloodGroup.O_POSITIVE);
        criticalReq.setUnitsRequired(3);
        criticalReq.setUrgency(BloodRequestUrgency.CRITICAL);
        criticalReq.setStatus(BloodRequestStatus.OPEN);
        criticalReq.setHospitalName("Lilavati Hospital");
        criticalReq.setHospitalAddress("A-791, Bandra Reclamation");
        criticalReq.setCity("Mumbai");
        criticalReq.setState("Maharashtra");
        criticalReq.setPostalCode("400050");
        criticalReq.setLatitude(19.0522);
        criticalReq.setLongitude(72.8295);
        criticalReq.setRequiredBy(Instant.now().plus(6, ChronoUnit.HOURS));
        criticalReq.setCreatedAt(Instant.now());
        criticalReq.setUpdatedAt(Instant.now());
        criticalReq = bloodRequestRepository.save(criticalReq);

        UpdateBloodRequestRequest updateReq = new UpdateBloodRequestRequest();
        updateReq.setUrgency(BloodRequestUrgency.NORMAL);

        mockMvc.perform(patch("/api/v1/blood-requests/" + criticalReq.getId())
                        .header("Authorization", "Bearer " + getAccessToken(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("Emergency requests must remain CRITICAL.")));

        BloodRequest unchanged = bloodRequestRepository.findById(criticalReq.getId()).orElseThrow();
        assertEquals(BloodRequestUrgency.CRITICAL, unchanged.getUrgency());
    }

    @Test
    @DisplayName("Update Blood Request - CRITICAL -> URGENT returns 400 VALIDATION_ERROR")
    void testUpdateBloodRequest_CriticalToUrgent_Rejected() throws Exception {
        BloodRequest criticalReq = new BloodRequest();
        criticalReq.setRequesterUserId(user1.getId());
        criticalReq.setBloodGroup(BloodGroup.O_POSITIVE);
        criticalReq.setUnitsRequired(3);
        criticalReq.setUrgency(BloodRequestUrgency.CRITICAL);
        criticalReq.setStatus(BloodRequestStatus.OPEN);
        criticalReq.setHospitalName("Lilavati Hospital");
        criticalReq.setHospitalAddress("A-791, Bandra Reclamation");
        criticalReq.setCity("Mumbai");
        criticalReq.setState("Maharashtra");
        criticalReq.setPostalCode("400050");
        criticalReq.setLatitude(19.0522);
        criticalReq.setLongitude(72.8295);
        criticalReq.setRequiredBy(Instant.now().plus(6, ChronoUnit.HOURS));
        criticalReq.setCreatedAt(Instant.now());
        criticalReq.setUpdatedAt(Instant.now());
        criticalReq = bloodRequestRepository.save(criticalReq);

        UpdateBloodRequestRequest updateReq = new UpdateBloodRequestRequest();
        updateReq.setUrgency(BloodRequestUrgency.URGENT);

        mockMvc.perform(patch("/api/v1/blood-requests/" + criticalReq.getId())
                        .header("Authorization", "Bearer " + getAccessToken(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("Emergency requests must remain CRITICAL.")));

        BloodRequest unchanged = bloodRequestRepository.findById(criticalReq.getId()).orElseThrow();
        assertEquals(BloodRequestUrgency.CRITICAL, unchanged.getUrgency());
    }

    @Test
    @DisplayName("Update Blood Request - ADMIN cannot convert normal request to CRITICAL (400 VALIDATION_ERROR)")
    void testUpdateBloodRequest_AdminCannotConvertToCritical() throws Exception {
        CreateBloodRequestRequest createReq = createSampleCreateRequest();
        createReq.setUrgency(BloodRequestUrgency.NORMAL);
        MvcResult createResult = mockMvc.perform(post("/api/v1/blood-requests")
                        .header("Authorization", "Bearer " + getAccessToken(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID requestId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());

        UpdateBloodRequestRequest updateReq = new UpdateBloodRequestRequest();
        updateReq.setUrgency(BloodRequestUrgency.CRITICAL);

        mockMvc.perform(patch("/api/v1/blood-requests/" + requestId)
                        .header("Authorization", "Bearer " + getAccessToken(adminUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("Critical urgency requests must be created through Emergency Mode.")));

        BloodRequest unchanged = bloodRequestRepository.findById(requestId).orElseThrow();
        assertEquals(BloodRequestUrgency.NORMAL, unchanged.getUrgency());
    }

    @Test
    @DisplayName("Create Blood Request - ADMIN cannot bypass CRITICAL urgency restriction via normal endpoint (400 VALIDATION_ERROR)")
    void testCreateBloodRequest_AdminCannotCreateCriticalViaNormalEndpoint() throws Exception {
        CreateBloodRequestRequest createReq = createSampleCreateRequest();
        createReq.setUrgency(BloodRequestUrgency.CRITICAL);

        mockMvc.perform(post("/api/v1/blood-requests")
                        .header("Authorization", "Bearer " + getAccessToken(adminUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("Critical urgency requests must be created through Emergency Mode.")));
    }

    @Test
    @DisplayName("Update Blood Request - Non-owner is forbidden (BOLA/IDOR protection)")
    void testUpdateBloodRequest_NonOwnerForbidden() throws Exception {
        CreateBloodRequestRequest createReq = createSampleCreateRequest();
        MvcResult createResult = mockMvc.perform(post("/api/v1/blood-requests")
                        .header("Authorization", "Bearer " + getAccessToken(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID requestId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());

        UpdateBloodRequestRequest updateReq = new UpdateBloodRequestRequest();
        updateReq.setUnitsRequired(10);

        mockMvc.perform(patch("/api/v1/blood-requests/" + requestId)
                        .header("Authorization", "Bearer " + getAccessToken(user2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Update Blood Request - Admin can manage any request")
    void testUpdateBloodRequest_AdminSuccess() throws Exception {
        CreateBloodRequestRequest createReq = createSampleCreateRequest();
        MvcResult createResult = mockMvc.perform(post("/api/v1/blood-requests")
                        .header("Authorization", "Bearer " + getAccessToken(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID requestId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());

        UpdateBloodRequestRequest updateReq = new UpdateBloodRequestRequest();
        updateReq.setUnitsRequired(8);

        mockMvc.perform(patch("/api/v1/blood-requests/" + requestId)
                        .header("Authorization", "Bearer " + getAccessToken(adminUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitsRequired").value(8));
    }

    @Test
    @DisplayName("Cancel Blood Request - Owner can cancel request and set cancellation reason")
    void testCancelBloodRequest_OwnerSuccess() throws Exception {
        CreateBloodRequestRequest createReq = createSampleCreateRequest();
        MvcResult createResult = mockMvc.perform(post("/api/v1/blood-requests")
                        .header("Authorization", "Bearer " + getAccessToken(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID requestId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());

        CancelBloodRequestRequest cancelReq = new CancelBloodRequestRequest();
        cancelReq.setReason("Patient received blood units from family donation.");

        mockMvc.perform(post("/api/v1/blood-requests/" + requestId + "/cancel")
                        .header("Authorization", "Bearer " + getAccessToken(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("Patient received blood units from family donation."))
                .andExpect(jsonPath("$.cancelledAt").isNotEmpty());

        BloodRequest cancelled = bloodRequestRepository.findById(requestId).orElseThrow();
        assertEquals(BloodRequestStatus.CANCELLED, cancelled.getStatus());
        assertEquals(user1.getId(), cancelled.getCancelledBy());
    }

    @Test
    @DisplayName("Cancel Blood Request - Non-owner cannot cancel another user's request")
    void testCancelBloodRequest_NonOwnerForbidden() throws Exception {
        CreateBloodRequestRequest createReq = createSampleCreateRequest();
        MvcResult createResult = mockMvc.perform(post("/api/v1/blood-requests")
                        .header("Authorization", "Bearer " + getAccessToken(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID requestId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(post("/api/v1/blood-requests/" + requestId + "/cancel")
                        .header("Authorization", "Bearer " + getAccessToken(user2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Terminal Blood Request - Cannot update or cancel once terminal")
    void testTerminalBloodRequest_CannotModify() throws Exception {
        CreateBloodRequestRequest createReq = createSampleCreateRequest();
        MvcResult createResult = mockMvc.perform(post("/api/v1/blood-requests")
                        .header("Authorization", "Bearer " + getAccessToken(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID requestId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());

        // Cancel first
        mockMvc.perform(post("/api/v1/blood-requests/" + requestId + "/cancel")
                        .header("Authorization", "Bearer " + getAccessToken(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        // Try updating cancelled request
        UpdateBloodRequestRequest updateReq = new UpdateBloodRequestRequest();
        updateReq.setUnitsRequired(10);
        mockMvc.perform(patch("/api/v1/blood-requests/" + requestId)
                        .header("Authorization", "Bearer " + getAccessToken(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isBadRequest());

        // Try cancelling again
        mockMvc.perform(post("/api/v1/blood-requests/" + requestId + "/cancel")
                        .header("Authorization", "Bearer " + getAccessToken(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Detail Privacy - Public / other user view masks requesterUserId, coordinates, and terminal metadata")
    void testDetailViewPrivacy() throws Exception {
        CreateBloodRequestRequest createReq = createSampleCreateRequest();
        MvcResult createResult = mockMvc.perform(post("/api/v1/blood-requests")
                        .header("Authorization", "Bearer " + getAccessToken(user1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID requestId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());

        // 1. Owner view includes coordinates, requesterUserId, description, isOwner=true, canManage=true
        mockMvc.perform(get("/api/v1/blood-requests/" + requestId)
                        .header("Authorization", "Bearer " + getAccessToken(user1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requesterUserId").value(user1.getId().toString()))
                .andExpect(jsonPath("$.latitude").value(18.9401))
                .andExpect(jsonPath("$.longitude").value(72.8347))
                .andExpect(jsonPath("$.description").value("Emergency blood units needed for planned heart surgery."))
                .andExpect(jsonPath("$.isOwner").value(true))
                .andExpect(jsonPath("$.canManage").value(true));

        // 2. Admin view includes coordinates, requesterUserId, description, isOwner=false, canManage=true
        mockMvc.perform(get("/api/v1/blood-requests/" + requestId)
                        .header("Authorization", "Bearer " + getAccessToken(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requesterUserId").value(user1.getId().toString()))
                .andExpect(jsonPath("$.latitude").value(18.9401))
                .andExpect(jsonPath("$.longitude").value(72.8347))
                .andExpect(jsonPath("$.description").value("Emergency blood units needed for planned heart surgery."))
                .andExpect(jsonPath("$.isOwner").value(false))
                .andExpect(jsonPath("$.canManage").value(true));

        // 3. Other authenticated user view MASKS coordinates, requesterUserId, description, and terminal metadata (isOwner=false, canManage=false)
        mockMvc.perform(get("/api/v1/blood-requests/" + requestId)
                        .header("Authorization", "Bearer " + getAccessToken(user2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requesterUserId").doesNotExist())
                .andExpect(jsonPath("$.latitude").doesNotExist())
                .andExpect(jsonPath("$.longitude").doesNotExist())
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.cancelledAt").doesNotExist())
                .andExpect(jsonPath("$.cancellationReason").doesNotExist())
                .andExpect(jsonPath("$.fulfilledAt").doesNotExist())
                .andExpect(jsonPath("$.isOwner").value(false))
                .andExpect(jsonPath("$.canManage").value(false))
                .andExpect(jsonPath("$.hospitalName").value("Apollo Memorial Hospital"));

        // 4. Anonymous view MASKS coordinates, requesterUserId, description, and terminal metadata (isOwner=false, canManage=false)
        mockMvc.perform(get("/api/v1/blood-requests/" + requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requesterUserId").doesNotExist())
                .andExpect(jsonPath("$.latitude").doesNotExist())
                .andExpect(jsonPath("$.longitude").doesNotExist())
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.cancelledAt").doesNotExist())
                .andExpect(jsonPath("$.cancellationReason").doesNotExist())
                .andExpect(jsonPath("$.fulfilledAt").doesNotExist())
                .andExpect(jsonPath("$.isOwner").value(false))
                .andExpect(jsonPath("$.canManage").value(false))
                .andExpect(jsonPath("$.hospitalName").value("Apollo Memorial Hospital"));
    }

    @Test
    @DisplayName("Harden Public Detail - Public/anonymous user can view OPEN request without sensitive data")
    void testPublicUserCanViewOpenRequest() throws Exception {
        BloodRequest req = new BloodRequest();
        req.setRequesterUserId(user1.getId());
        req.setBloodGroup(BloodGroup.B_POSITIVE);
        req.setUnitsRequired(2);
        req.setUrgency(BloodRequestUrgency.NORMAL);
        req.setStatus(BloodRequestStatus.OPEN);
        req.setHospitalName("City Care Hospital");
        req.setHospitalAddress("50 Hospital Road");
        req.setCity("Mumbai");
        req.setState("Maharashtra");
        req.setPostalCode("400001");
        req.setLatitude(18.9401);
        req.setLongitude(72.8347);
        req.setRequiredBy(Instant.now().plus(24, ChronoUnit.HOURS));
        req.setDescription("Sensitive notes regarding patient diagnosis");
        req = bloodRequestRepository.save(req);

        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(req.getId().toString()))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.requesterUserId").doesNotExist())
                .andExpect(jsonPath("$.latitude").doesNotExist())
                .andExpect(jsonPath("$.longitude").doesNotExist())
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.isOwner").value(false))
                .andExpect(jsonPath("$.canManage").value(false));
    }

    @Test
    @DisplayName("Public Description Privacy - Public GET of Blood Request never returns description, coordinates, or requester identity")
    void testPublicGetNeverReturnsDescription() throws Exception {
        BloodRequest req = new BloodRequest();
        req.setRequesterUserId(user1.getId());
        req.setBloodGroup(BloodGroup.AB_NEGATIVE);
        req.setUnitsRequired(4);
        req.setUrgency(BloodRequestUrgency.CRITICAL);
        req.setStatus(BloodRequestStatus.OPEN);
        req.setHospitalName("Tata Memorial Hospital");
        req.setHospitalAddress("Dr. E Borges Road, Parel");
        req.setCity("Mumbai");
        req.setState("Maharashtra");
        req.setPostalCode("400012");
        req.setLatitude(19.0048);
        req.setLongitude(72.8427);
        req.setRequiredBy(Instant.now().plus(12, ChronoUnit.HOURS));
        req.setDescription("Sensitive patient clinical condition: Advanced acute leukemia in ICU bed 12.");
        req = bloodRequestRepository.save(req);

        // 1. Anonymous public user: description and sensitive fields NEVER returned (isOwner=false, canManage=false)
        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(req.getId().toString()))
                .andExpect(jsonPath("$.bloodGroup").value("AB-"))
                .andExpect(jsonPath("$.unitsRequired").value(4))
                .andExpect(jsonPath("$.urgency").value("CRITICAL"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.hospitalName").value("Tata Memorial Hospital"))
                .andExpect(jsonPath("$.hospitalAddress").value("Dr. E Borges Road, Parel"))
                .andExpect(jsonPath("$.city").value("Mumbai"))
                .andExpect(jsonPath("$.state").value("Maharashtra"))
                .andExpect(jsonPath("$.postalCode").value("400012"))
                .andExpect(jsonPath("$.isOwner").value(false))
                .andExpect(jsonPath("$.canManage").value(false))
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.requesterUserId").doesNotExist())
                .andExpect(jsonPath("$.latitude").doesNotExist())
                .andExpect(jsonPath("$.longitude").doesNotExist())
                .andExpect(jsonPath("$.cancelledAt").doesNotExist())
                .andExpect(jsonPath("$.cancellationReason").doesNotExist())
                .andExpect(jsonPath("$.fulfilledAt").doesNotExist())
                .andExpect(jsonPath("$.updatedAt").doesNotExist());

        // 2. Non-owner authenticated user: description and sensitive fields NEVER returned (isOwner=false, canManage=false)
        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId())
                        .header("Authorization", "Bearer " + getAccessToken(user2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.requesterUserId").doesNotExist())
                .andExpect(jsonPath("$.latitude").doesNotExist())
                .andExpect(jsonPath("$.longitude").doesNotExist())
                .andExpect(jsonPath("$.isOwner").value(false))
                .andExpect(jsonPath("$.canManage").value(false));

        // 3. Owner: legitimately sees description, coordinates, requesterUserId (isOwner=true, canManage=true)
        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId())
                        .header("Authorization", "Bearer " + getAccessToken(user1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Sensitive patient clinical condition: Advanced acute leukemia in ICU bed 12."))
                .andExpect(jsonPath("$.requesterUserId").value(user1.getId().toString()))
                .andExpect(jsonPath("$.latitude").value(19.0048))
                .andExpect(jsonPath("$.longitude").value(72.8427))
                .andExpect(jsonPath("$.isOwner").value(true))
                .andExpect(jsonPath("$.canManage").value(true));

        // 4. Admin viewing another user's request: legitimately sees description, coordinates, requesterUserId (isOwner=false, canManage=true)
        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId())
                        .header("Authorization", "Bearer " + getAccessToken(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Sensitive patient clinical condition: Advanced acute leukemia in ICU bed 12."))
                .andExpect(jsonPath("$.requesterUserId").value(user1.getId().toString()))
                .andExpect(jsonPath("$.latitude").value(19.0048))
                .andExpect(jsonPath("$.longitude").value(72.8427))
                .andExpect(jsonPath("$.isOwner").value(false))
                .andExpect(jsonPath("$.canManage").value(true));
    }

    @Test
    @DisplayName("Harden Public Detail - Public user cannot view CANCELLED request by direct UUID (returns 404)")
    void testPublicUserCannotViewCancelledRequestByDirectUuid() throws Exception {
        BloodRequest req = new BloodRequest();
        req.setRequesterUserId(user1.getId());
        req.setBloodGroup(BloodGroup.O_POSITIVE);
        req.setUnitsRequired(1);
        req.setUrgency(BloodRequestUrgency.NORMAL);
        req.setStatus(BloodRequestStatus.CANCELLED);
        req.setCancelledAt(Instant.now());
        req.setCancelledBy(user1.getId());
        req.setCancellationReason("Fulfilled by family");
        req.setHospitalName("City Care Hospital");
        req.setHospitalAddress("50 Hospital Road");
        req.setCity("Mumbai");
        req.setState("Maharashtra");
        req.setPostalCode("400001");
        req.setLatitude(18.9401);
        req.setLongitude(72.8347);
        req.setRequiredBy(Instant.now().plus(24, ChronoUnit.HOURS));
        req = bloodRequestRepository.save(req);

        // Anonymous user -> 404
        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId()))
                .andExpect(status().isNotFound());

        // Non-owner authenticated user -> 404
        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId())
                        .header("Authorization", "Bearer " + getAccessToken(user2)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Harden Public Detail - Public user cannot view EXPIRED request by direct UUID (returns 404)")
    void testPublicUserCannotViewExpiredRequestByDirectUuid() throws Exception {
        BloodRequest req = new BloodRequest();
        req.setRequesterUserId(user1.getId());
        req.setBloodGroup(BloodGroup.A_NEGATIVE);
        req.setUnitsRequired(2);
        req.setUrgency(BloodRequestUrgency.CRITICAL);
        req.setStatus(BloodRequestStatus.EXPIRED);
        req.setHospitalName("City Care Hospital");
        req.setHospitalAddress("50 Hospital Road");
        req.setCity("Mumbai");
        req.setState("Maharashtra");
        req.setPostalCode("400001");
        req.setLatitude(18.9401);
        req.setLongitude(72.8347);
        req.setRequiredBy(Instant.now().minus(2, ChronoUnit.HOURS));
        req = bloodRequestRepository.save(req);

        // Anonymous user -> 404
        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId()))
                .andExpect(status().isNotFound());

        // Non-owner authenticated user -> 404
        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId())
                        .header("Authorization", "Bearer " + getAccessToken(user2)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Harden Public Detail - Public user cannot view FULFILLED request by direct UUID (returns 404)")
    void testPublicUserCannotViewFulfilledRequestByDirectUuid() throws Exception {
        BloodRequest req = new BloodRequest();
        req.setRequesterUserId(user1.getId());
        req.setBloodGroup(BloodGroup.AB_POSITIVE);
        req.setUnitsRequired(3);
        req.setUrgency(BloodRequestUrgency.NORMAL);
        req.setStatus(BloodRequestStatus.FULFILLED);
        req.setFulfilledAt(Instant.now());
        req.setHospitalName("City Care Hospital");
        req.setHospitalAddress("50 Hospital Road");
        req.setCity("Mumbai");
        req.setState("Maharashtra");
        req.setPostalCode("400001");
        req.setLatitude(18.9401);
        req.setLongitude(72.8347);
        req.setRequiredBy(Instant.now().plus(24, ChronoUnit.HOURS));
        req = bloodRequestRepository.save(req);

        // Anonymous user -> 404
        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId()))
                .andExpect(status().isNotFound());

        // Non-owner authenticated user -> 404
        mockMvc.perform(get("/api/v1/blood-requests/" + req.getId())
                        .header("Authorization", "Bearer " + getAccessToken(user2)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Harden Public Detail - Owner can still view own terminal requests (CANCELLED, EXPIRED, FULFILLED)")
    void testOwnerCanStillViewOwnTerminalRequest() throws Exception {
        // Cancelled
        BloodRequest cancelled = new BloodRequest();
        cancelled.setRequesterUserId(user1.getId());
        cancelled.setBloodGroup(BloodGroup.O_POSITIVE);
        cancelled.setUnitsRequired(1);
        cancelled.setStatus(BloodRequestStatus.CANCELLED);
        cancelled.setCancelledAt(Instant.now());
        cancelled.setCancelledBy(user1.getId());
        cancelled.setCancellationReason("Donation completed");
        cancelled.setHospitalName("Hospital A");
        cancelled.setHospitalAddress("Address A");
        cancelled.setCity("Mumbai");
        cancelled.setState("Maharashtra");
        cancelled.setPostalCode("400001");
        cancelled.setLatitude(18.9401);
        cancelled.setLongitude(72.8347);
        cancelled.setRequiredBy(Instant.now().plus(12, ChronoUnit.HOURS));
        cancelled = bloodRequestRepository.save(cancelled);

        mockMvc.perform(get("/api/v1/blood-requests/" + cancelled.getId())
                        .header("Authorization", "Bearer " + getAccessToken(user1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.isOwner").value(true))
                .andExpect(jsonPath("$.requesterUserId").value(user1.getId().toString()))
                .andExpect(jsonPath("$.latitude").value(18.9401))
                .andExpect(jsonPath("$.cancelledAt").isNotEmpty())
                .andExpect(jsonPath("$.cancellationReason").value("Donation completed"));

        // Expired
        BloodRequest expired = new BloodRequest();
        expired.setRequesterUserId(user1.getId());
        expired.setBloodGroup(BloodGroup.B_NEGATIVE);
        expired.setUnitsRequired(2);
        expired.setStatus(BloodRequestStatus.EXPIRED);
        expired.setHospitalName("Hospital B");
        expired.setHospitalAddress("Address B");
        expired.setCity("Mumbai");
        expired.setState("Maharashtra");
        expired.setPostalCode("400001");
        expired.setLatitude(18.9401);
        expired.setLongitude(72.8347);
        expired.setRequiredBy(Instant.now().minus(2, ChronoUnit.HOURS));
        expired = bloodRequestRepository.save(expired);

        mockMvc.perform(get("/api/v1/blood-requests/" + expired.getId())
                        .header("Authorization", "Bearer " + getAccessToken(user1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXPIRED"))
                .andExpect(jsonPath("$.isOwner").value(true));

        // Fulfilled
        BloodRequest fulfilled = new BloodRequest();
        fulfilled.setRequesterUserId(user1.getId());
        fulfilled.setBloodGroup(BloodGroup.AB_POSITIVE);
        fulfilled.setUnitsRequired(1);
        fulfilled.setStatus(BloodRequestStatus.FULFILLED);
        fulfilled.setFulfilledAt(Instant.now());
        fulfilled.setHospitalName("Hospital C");
        fulfilled.setHospitalAddress("Address C");
        fulfilled.setCity("Mumbai");
        fulfilled.setState("Maharashtra");
        fulfilled.setPostalCode("400001");
        fulfilled.setLatitude(18.9401);
        fulfilled.setLongitude(72.8347);
        fulfilled.setRequiredBy(Instant.now().plus(24, ChronoUnit.HOURS));
        fulfilled = bloodRequestRepository.save(fulfilled);

        mockMvc.perform(get("/api/v1/blood-requests/" + fulfilled.getId())
                        .header("Authorization", "Bearer " + getAccessToken(user1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FULFILLED"))
                .andExpect(jsonPath("$.isOwner").value(true))
                .andExpect(jsonPath("$.fulfilledAt").isNotEmpty());
    }

    @Test
    @DisplayName("Harden Public Detail - Admin can still view any terminal request")
    void testAdminCanStillViewTerminalRequest() throws Exception {
        BloodRequest cancelled = new BloodRequest();
        cancelled.setRequesterUserId(user1.getId());
        cancelled.setBloodGroup(BloodGroup.O_POSITIVE);
        cancelled.setUnitsRequired(1);
        cancelled.setStatus(BloodRequestStatus.CANCELLED);
        cancelled.setCancelledAt(Instant.now());
        cancelled.setCancelledBy(user1.getId());
        cancelled.setCancellationReason("Patient discharged");
        cancelled.setHospitalName("Hospital Admin Test");
        cancelled.setHospitalAddress("Address Admin");
        cancelled.setCity("Mumbai");
        cancelled.setState("Maharashtra");
        cancelled.setPostalCode("400001");
        cancelled.setLatitude(18.9401);
        cancelled.setLongitude(72.8347);
        cancelled.setRequiredBy(Instant.now().plus(12, ChronoUnit.HOURS));
        cancelled = bloodRequestRepository.save(cancelled);

        mockMvc.perform(get("/api/v1/blood-requests/" + cancelled.getId())
                        .header("Authorization", "Bearer " + getAccessToken(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.isOwner").value(false))
                .andExpect(jsonPath("$.canManage").value(true))
                .andExpect(jsonPath("$.requesterUserId").value(user1.getId().toString()))
                .andExpect(jsonPath("$.latitude").value(18.9401))
                .andExpect(jsonPath("$.cancelledAt").isNotEmpty());
    }

    @Test
    @DisplayName("Public Discovery - Only OPEN requests are discoverable, CANCELLED/EXPIRED are excluded")
    void testPublicDiscovery_OnlyOpenReturned() throws Exception {
        BloodRequest openReq = new BloodRequest();
        openReq.setRequesterUserId(user1.getId());
        openReq.setBloodGroup(BloodGroup.A_POSITIVE);
        openReq.setUnitsRequired(2);
        openReq.setUrgency(BloodRequestUrgency.NORMAL);
        openReq.setStatus(BloodRequestStatus.OPEN);
        openReq.setHospitalName("City General Hospital");
        openReq.setHospitalAddress("100 Central Road");
        openReq.setCity("Pune");
        openReq.setState("Maharashtra");
        openReq.setPostalCode("411001");
        openReq.setLatitude(18.5204);
        openReq.setLongitude(73.8567);
        openReq.setRequiredBy(Instant.now().plus(24, ChronoUnit.HOURS));
        openReq = bloodRequestRepository.save(openReq);

        BloodRequest cancelledReq = new BloodRequest();
        cancelledReq.setRequesterUserId(user1.getId());
        cancelledReq.setBloodGroup(BloodGroup.A_POSITIVE);
        cancelledReq.setUnitsRequired(4);
        cancelledReq.setUrgency(BloodRequestUrgency.URGENT);
        cancelledReq.setStatus(BloodRequestStatus.CANCELLED);
        cancelledReq.setHospitalName("City General Hospital");
        cancelledReq.setHospitalAddress("100 Central Road");
        cancelledReq.setCity("Pune");
        cancelledReq.setState("Maharashtra");
        cancelledReq.setPostalCode("411001");
        cancelledReq.setLatitude(18.5204);
        cancelledReq.setLongitude(73.8567);
        cancelledReq.setRequiredBy(Instant.now().plus(24, ChronoUnit.HOURS));
        cancelledReq = bloodRequestRepository.save(cancelledReq);

        BloodRequest expiredReq = new BloodRequest();
        expiredReq.setRequesterUserId(user1.getId());
        expiredReq.setBloodGroup(BloodGroup.A_POSITIVE);
        expiredReq.setUnitsRequired(1);
        expiredReq.setUrgency(BloodRequestUrgency.NORMAL);
        expiredReq.setStatus(BloodRequestStatus.EXPIRED);
        expiredReq.setHospitalName("City General Hospital");
        expiredReq.setHospitalAddress("100 Central Road");
        expiredReq.setCity("Pune");
        expiredReq.setState("Maharashtra");
        expiredReq.setPostalCode("411001");
        expiredReq.setLatitude(18.5204);
        expiredReq.setLongitude(73.8567);
        expiredReq.setRequiredBy(Instant.now().minus(2, ChronoUnit.HOURS));
        expiredReq = bloodRequestRepository.save(expiredReq);

        // Discovery without filter
        mockMvc.perform(get("/api/v1/blood-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(openReq.getId().toString()))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"));

        // Discovery with filter
        mockMvc.perform(get("/api/v1/blood-requests")
                        .param("bloodGroup", "A+")
                        .param("city", "pune")
                        .param("urgency", "NORMAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(openReq.getId().toString()));
    }

    @Test
    @DisplayName("My Requests - Returns user's own requests across all statuses")
    void testGetMyRequests() throws Exception {
        BloodRequest r1 = new BloodRequest();
        r1.setRequesterUserId(user1.getId());
        r1.setBloodGroup(BloodGroup.B_POSITIVE);
        r1.setUnitsRequired(2);
        r1.setUrgency(BloodRequestUrgency.NORMAL);
        r1.setStatus(BloodRequestStatus.OPEN);
        r1.setHospitalName("Hospital A");
        r1.setHospitalAddress("Addr A");
        r1.setCity("Delhi");
        r1.setState("Delhi");
        r1.setPostalCode("110001");
        r1.setLatitude(28.6139);
        r1.setLongitude(77.2090);
        r1.setRequiredBy(Instant.now().plus(12, ChronoUnit.HOURS));
        bloodRequestRepository.save(r1);

        BloodRequest r2 = new BloodRequest();
        r2.setRequesterUserId(user1.getId());
        r2.setBloodGroup(BloodGroup.O_NEGATIVE);
        r2.setUnitsRequired(1);
        r2.setUrgency(BloodRequestUrgency.CRITICAL);
        r2.setStatus(BloodRequestStatus.CANCELLED);
        r2.setHospitalName("Hospital B");
        r2.setHospitalAddress("Addr B");
        r2.setCity("Delhi");
        r2.setState("Delhi");
        r2.setPostalCode("110001");
        r2.setLatitude(28.6139);
        r2.setLongitude(77.2090);
        r2.setRequiredBy(Instant.now().plus(12, ChronoUnit.HOURS));
        bloodRequestRepository.save(r2);

        BloodRequest rOther = new BloodRequest();
        rOther.setRequesterUserId(user2.getId());
        rOther.setBloodGroup(BloodGroup.AB_POSITIVE);
        rOther.setUnitsRequired(3);
        rOther.setStatus(BloodRequestStatus.OPEN);
        rOther.setHospitalName("Hospital C");
        rOther.setHospitalAddress("Addr C");
        rOther.setCity("Delhi");
        rOther.setState("Delhi");
        rOther.setPostalCode("110001");
        rOther.setLatitude(28.6139);
        rOther.setLongitude(77.2090);
        rOther.setRequiredBy(Instant.now().plus(12, ChronoUnit.HOURS));
        bloodRequestRepository.save(rOther);

        mockMvc.perform(get("/api/v1/blood-requests/me")
                        .header("Authorization", "Bearer " + getAccessToken(user1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    @DisplayName("Nearby Discovery - Returns requests within specified radius ordered by distance")
    void testNearbyDiscovery() throws Exception {
        // Center: Mumbai (18.9401, 72.8347)
        // Request 1: ~2km away (18.9500, 72.8347)
        BloodRequest rClose = new BloodRequest();
        rClose.setRequesterUserId(user1.getId());
        rClose.setBloodGroup(BloodGroup.O_POSITIVE);
        rClose.setUnitsRequired(2);
        rClose.setStatus(BloodRequestStatus.OPEN);
        rClose.setHospitalName("Close Hospital");
        rClose.setHospitalAddress("Near Town");
        rClose.setCity("Mumbai");
        rClose.setState("Maharashtra");
        rClose.setPostalCode("400001");
        rClose.setLatitude(18.9500);
        rClose.setLongitude(72.8347);
        rClose.setRequiredBy(Instant.now().plus(24, ChronoUnit.HOURS));
        rClose = bloodRequestRepository.save(rClose);

        // Request 2: ~8km away (19.0000, 72.8347)
        BloodRequest rMid = new BloodRequest();
        rMid.setRequesterUserId(user1.getId());
        rMid.setBloodGroup(BloodGroup.O_POSITIVE);
        rMid.setUnitsRequired(3);
        rMid.setStatus(BloodRequestStatus.OPEN);
        rMid.setHospitalName("Mid Hospital");
        rMid.setHospitalAddress("Mid Town");
        rMid.setCity("Mumbai");
        rMid.setState("Maharashtra");
        rMid.setPostalCode("400012");
        rMid.setLatitude(19.0000);
        rMid.setLongitude(72.8347);
        rMid.setRequiredBy(Instant.now().plus(24, ChronoUnit.HOURS));
        rMid = bloodRequestRepository.save(rMid);

        // Request 3: ~120km away (Pune 18.5204, 73.8567)
        BloodRequest rFar = new BloodRequest();
        rFar.setRequesterUserId(user1.getId());
        rFar.setBloodGroup(BloodGroup.O_POSITIVE);
        rFar.setUnitsRequired(1);
        rFar.setStatus(BloodRequestStatus.OPEN);
        rFar.setHospitalName("Far Hospital");
        rFar.setHospitalAddress("Far City");
        rFar.setCity("Pune");
        rFar.setState("Maharashtra");
        rFar.setPostalCode("411001");
        rFar.setLatitude(18.5204);
        rFar.setLongitude(73.8567);
        rFar.setRequiredBy(Instant.now().plus(24, ChronoUnit.HOURS));
        rFar = bloodRequestRepository.save(rFar);

        // Search with radius 15km: should return rClose and rMid, sorted by distance
        mockMvc.perform(get("/api/v1/blood-requests/nearby")
                        .param("latitude", "18.9401")
                        .param("longitude", "72.8347")
                        .param("radiusKm", "15.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(rClose.getId().toString()))
                .andExpect(jsonPath("$[1].id").value(rMid.getId().toString()));
    }

    @Test
    @DisplayName("Expiration - Overdue OPEN requests transition to EXPIRED with version increment; idempotent")
    void testExpiration() {
        Instant now = Instant.now();

        BloodRequest overdue = new BloodRequest();
        overdue.setRequesterUserId(user1.getId());
        overdue.setBloodGroup(BloodGroup.A_POSITIVE);
        overdue.setUnitsRequired(2);
        overdue.setStatus(BloodRequestStatus.OPEN);
        overdue.setHospitalName("Overdue Hospital");
        overdue.setHospitalAddress("Some Address");
        overdue.setCity("Mumbai");
        overdue.setState("Maharashtra");
        overdue.setPostalCode("400001");
        overdue.setLatitude(18.9401);
        overdue.setLongitude(72.8347);
        overdue.setRequiredBy(now.minus(1, ChronoUnit.HOURS));
        overdue = bloodRequestRepository.save(overdue);
        assertEquals(0L, overdue.getVersion());

        BloodRequest active = new BloodRequest();
        active.setRequesterUserId(user1.getId());
        active.setBloodGroup(BloodGroup.A_POSITIVE);
        active.setUnitsRequired(2);
        active.setStatus(BloodRequestStatus.OPEN);
        active.setHospitalName("Active Hospital");
        active.setHospitalAddress("Some Address");
        active.setCity("Mumbai");
        active.setState("Maharashtra");
        active.setPostalCode("400001");
        active.setLatitude(18.9401);
        active.setLongitude(72.8347);
        active.setRequiredBy(now.plus(24, ChronoUnit.HOURS));
        active = bloodRequestRepository.save(active);

        // Run expiration
        int expired = expirationService.processExpirations(now);
        assertEquals(1, expired);

        BloodRequest updatedOverdue = bloodRequestRepository.findById(overdue.getId()).orElseThrow();
        assertEquals(BloodRequestStatus.EXPIRED, updatedOverdue.getStatus());
        assertEquals(1L, updatedOverdue.getVersion());

        BloodRequest updatedActive = bloodRequestRepository.findById(active.getId()).orElseThrow();
        assertEquals(BloodRequestStatus.OPEN, updatedActive.getStatus());
        assertEquals(0L, updatedActive.getVersion());

        // Idempotent second execution
        int secondRun = expirationService.processExpirations(now);
        assertEquals(0, secondRun);
    }

    @Test
    @DisplayName("Defense against stale requests - Future OPEN request appears in public discovery, overdue OPEN request does NOT")
    void testOverdueOpenRequestExcludedFromPublicDiscovery() throws Exception {
        // 1. Future OPEN request
        BloodRequest futureOpen = new BloodRequest();
        futureOpen.setRequesterUserId(user1.getId());
        futureOpen.setBloodGroup(BloodGroup.O_POSITIVE);
        futureOpen.setUnitsRequired(2);
        futureOpen.setStatus(BloodRequestStatus.OPEN);
        futureOpen.setHospitalName("Future Hospital");
        futureOpen.setHospitalAddress("10 Future Road");
        futureOpen.setCity("Mumbai");
        futureOpen.setState("Maharashtra");
        futureOpen.setPostalCode("400001");
        futureOpen.setLatitude(18.9401);
        futureOpen.setLongitude(72.8347);
        futureOpen.setRequiredBy(Instant.now().plus(24, ChronoUnit.HOURS));
        futureOpen = bloodRequestRepository.save(futureOpen);

        // 2. Overdue OPEN request (requiredBy in past, but scheduled expiration hasn't run yet)
        BloodRequest pastOpen = new BloodRequest();
        pastOpen.setRequesterUserId(user1.getId());
        pastOpen.setBloodGroup(BloodGroup.O_POSITIVE);
        pastOpen.setUnitsRequired(2);
        pastOpen.setStatus(BloodRequestStatus.OPEN);
        pastOpen.setHospitalName("Past Hospital");
        pastOpen.setHospitalAddress("20 Past Road");
        pastOpen.setCity("Mumbai");
        pastOpen.setState("Maharashtra");
        pastOpen.setPostalCode("400001");
        pastOpen.setLatitude(18.9401);
        pastOpen.setLongitude(72.8347);
        pastOpen.setRequiredBy(Instant.now().minus(2, ChronoUnit.HOURS));
        pastOpen = bloodRequestRepository.save(pastOpen);

        mockMvc.perform(get("/api/v1/blood-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(futureOpen.getId().toString()))
                .andExpect(jsonPath("$.content[0].hospitalName").value("Future Hospital"));
    }

    @Test
    @DisplayName("Defense against stale requests - Overdue OPEN request does NOT appear in nearby discovery")
    void testOverdueOpenRequestExcludedFromNearbyDiscovery() throws Exception {
        // 1. Future OPEN request within 2km
        BloodRequest futureNearby = new BloodRequest();
        futureNearby.setRequesterUserId(user1.getId());
        futureNearby.setBloodGroup(BloodGroup.A_POSITIVE);
        futureNearby.setUnitsRequired(1);
        futureNearby.setStatus(BloodRequestStatus.OPEN);
        futureNearby.setHospitalName("Future Nearby Hospital");
        futureNearby.setHospitalAddress("10 Nearby Road");
        futureNearby.setCity("Mumbai");
        futureNearby.setState("Maharashtra");
        futureNearby.setPostalCode("400001");
        futureNearby.setLatitude(18.9500);
        futureNearby.setLongitude(72.8347);
        futureNearby.setRequiredBy(Instant.now().plus(12, ChronoUnit.HOURS));
        futureNearby = bloodRequestRepository.save(futureNearby);

        // 2. Overdue OPEN request within 2km (requiredBy in past, but scheduled expiration hasn't run yet)
        BloodRequest pastNearby = new BloodRequest();
        pastNearby.setRequesterUserId(user1.getId());
        pastNearby.setBloodGroup(BloodGroup.A_POSITIVE);
        pastNearby.setUnitsRequired(1);
        pastNearby.setStatus(BloodRequestStatus.OPEN);
        pastNearby.setHospitalName("Past Nearby Hospital");
        pastNearby.setHospitalAddress("20 Nearby Road");
        pastNearby.setCity("Mumbai");
        pastNearby.setState("Maharashtra");
        pastNearby.setPostalCode("400001");
        pastNearby.setLatitude(18.9500);
        pastNearby.setLongitude(72.8347);
        pastNearby.setRequiredBy(Instant.now().minus(1, ChronoUnit.HOURS));
        pastNearby = bloodRequestRepository.save(pastNearby);

        mockMvc.perform(get("/api/v1/blood-requests/nearby")
                        .param("latitude", "18.9401")
                        .param("longitude", "72.8347")
                        .param("radiusKm", "10.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(futureNearby.getId().toString()))
                .andExpect(jsonPath("$[0].hospitalName").value("Future Nearby Hospital"));
    }

    @Test
    @DisplayName("Defense against stale requests - Public direct detail of an overdue OPEN request returns 404, but owner and admin can view it")
    void testOverdueOpenRequestDirectDetailAccess() throws Exception {
        BloodRequest overdueOpen = new BloodRequest();
        overdueOpen.setRequesterUserId(user1.getId());
        overdueOpen.setBloodGroup(BloodGroup.B_POSITIVE);
        overdueOpen.setUnitsRequired(2);
        overdueOpen.setStatus(BloodRequestStatus.OPEN);
        overdueOpen.setHospitalName("Overdue Open Hospital");
        overdueOpen.setHospitalAddress("30 Overdue Road");
        overdueOpen.setCity("Mumbai");
        overdueOpen.setState("Maharashtra");
        overdueOpen.setPostalCode("400001");
        overdueOpen.setLatitude(18.9401);
        overdueOpen.setLongitude(72.8347);
        overdueOpen.setRequiredBy(Instant.now().minus(30, ChronoUnit.MINUTES));
        overdueOpen = bloodRequestRepository.save(overdueOpen);

        // 1. Anonymous user -> 404
        mockMvc.perform(get("/api/v1/blood-requests/" + overdueOpen.getId()))
                .andExpect(status().isNotFound());

        // 2. Non-owner authenticated user -> 404
        mockMvc.perform(get("/api/v1/blood-requests/" + overdueOpen.getId())
                        .header("Authorization", "Bearer " + getAccessToken(user2)))
                .andExpect(status().isNotFound());

        // 3. Owner -> 200 OK with isOwner = true, canManage = true
        mockMvc.perform(get("/api/v1/blood-requests/" + overdueOpen.getId())
                        .header("Authorization", "Bearer " + getAccessToken(user1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(overdueOpen.getId().toString()))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.isOwner").value(true))
                .andExpect(jsonPath("$.canManage").value(true))
                .andExpect(jsonPath("$.requesterUserId").value(user1.getId().toString()))
                .andExpect(jsonPath("$.latitude").value(18.9401));

        // 4. Admin -> 200 OK with isOwner = false, canManage = true
        mockMvc.perform(get("/api/v1/blood-requests/" + overdueOpen.getId())
                        .header("Authorization", "Bearer " + getAccessToken(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(overdueOpen.getId().toString()))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.isOwner").value(false))
                .andExpect(jsonPath("$.canManage").value(true))
                .andExpect(jsonPath("$.requesterUserId").value(user1.getId().toString()))
                .andExpect(jsonPath("$.latitude").value(18.9401));
    }
}
