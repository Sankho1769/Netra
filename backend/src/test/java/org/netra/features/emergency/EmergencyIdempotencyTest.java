package org.netra.features.emergency;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.core.audit.AuditService;
import org.netra.core.audit.SecurityAuditLog;
import org.netra.core.audit.SecurityAuditLogRepository;
import org.netra.core.ratelimit.RateLimitingService;
import org.netra.core.security.JwtTokenProvider;
import org.netra.features.bloodrequest.entity.BloodRequest;
import org.netra.features.bloodrequest.entity.BloodRequestStatus;
import org.netra.features.bloodrequest.entity.BloodRequestUrgency;
import org.netra.features.bloodrequest.repository.BloodRequestRepository;
import org.netra.features.bloodrequest.service.BloodRequestService;
import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.emergency.dto.EmergencyBloodRequestRequest;
import org.netra.features.emergency.entity.IdempotencyRecord;
import org.netra.features.emergency.repository.IdempotencyRecordRepository;
import org.netra.features.emergency.service.EmergencyIdempotencyService;
import org.netra.features.emergency.service.EmergencyService;
import org.netra.features.emergency.service.EmergencyTransactionalService;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.repository.UserRepository;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EmergencyIdempotencyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

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

    @Autowired
    private EmergencyService emergencyService;

    @Autowired
    private BloodRequestService bloodRequestService;

    @Autowired
    private AuditService auditService;

    private User testUser;
    private String userToken;

    @BeforeEach
    void setUp() {
        securityAuditLogRepository.deleteAll();
        bloodRequestRepository.deleteAll();
        idempotencyRecordRepository.deleteAll();
        rateLimitingService.reset();

        testUser = createTestUser("idemp.tester", UserRole.ROLE_RECEIVER);
        userToken = jwtTokenProvider.generateAccessToken(testUser.getId(), List.of("ROLE_RECEIVER"));
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

    private EmergencyBloodRequestRequest buildRequest() {
        return new EmergencyBloodRequestRequest(
                BloodGroup.O_POSITIVE,
                3,
                "Lilavati Hospital",
                "A-791, Bandra Reclamation",
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
    @DisplayName("Creation requires Idempotency-Key header; missing header returns 400 Bad Request")
    void testMissingIdempotencyKey_Rejected() throws Exception {
        EmergencyBloodRequestRequest request = buildRequest();

        mockMvc.perform(post("/api/v1/emergency/blood-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("Idempotency-Key header is required")));

        // Empty string key also rejected
        mockMvc.perform(post("/api/v1/emergency/blood-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .header("Idempotency-Key", "   ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Idempotency-Key header is required")));
    }

    @Test
    @DisplayName("Oversized Idempotency-Key (>255 chars) returns 400 Bad Request with VALIDATION_ERROR")
    void testOversizedIdempotencyKey_Rejected() throws Exception {
        EmergencyBloodRequestRequest request = buildRequest();
        String oversizedKey = "k".repeat(256);

        mockMvc.perform(post("/api/v1/emergency/blood-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .header("Idempotency-Key", oversizedKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("Idempotency-Key header must not exceed 255 characters.")));

        assertEquals(0, bloodRequestRepository.count());
        assertEquals(0, idempotencyRecordRepository.count());
    }

    @Test
    @DisplayName("First request returns 201 Created; replay with same key and payload returns 200 OK without duplicate insert or audit")
    void testIdempotency_SuccessAndReplay() throws Exception {
        String key = "key-" + UUID.randomUUID();
        EmergencyBloodRequestRequest request = buildRequest();

        // 1. First submission -> 201 Created
        MvcResult firstResult = mockMvc.perform(post("/api/v1/emergency/blood-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.urgency").value("CRITICAL"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn();

        JsonNode firstJson = objectMapper.readTree(firstResult.getResponse().getContentAsString());
        String originalRequestId = firstJson.get("id").asText();

        // Verify exactly one BloodRequest and one IdempotencyRecord
        assertEquals(1, bloodRequestRepository.count());
        assertEquals(1, idempotencyRecordRepository.count());

        long initialAuditCount = securityAuditLogRepository.findAll().stream()
                .filter(l -> "EMERGENCY_REQUEST_CREATED".equals(l.getEventType()))
                .count();
        assertEquals(1, initialAuditCount);

        // 2. Replay with identical key and payload -> 200 OK
        MvcResult replayResult = mockMvc.perform(post("/api/v1/emergency/blood-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(originalRequestId))
                .andReturn();

        // Verify still exactly one BloodRequest in database
        assertEquals(1, bloodRequestRepository.count());

        // Verify no second creation audit event emitted
        long replayAuditCount = securityAuditLogRepository.findAll().stream()
                .filter(l -> "EMERGENCY_REQUEST_CREATED".equals(l.getEventType()))
                .count();
        assertEquals(1, replayAuditCount, "Replay must NOT emit a duplicate creation audit event");

        // Verify replay does NOT consume a rate limit slot (we can still make 4 more requests!)
        for (int i = 1; i <= 4; i++) {
            EmergencyBloodRequestRequest req = buildRequest();
            req.setHospitalName("Hospital " + i);
            mockMvc.perform(post("/api/v1/emergency/blood-requests")
                            .header("Authorization", "Bearer " + userToken)
                            .header("Idempotency-Key", "other-key-" + i)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    @DisplayName("Same key with different payload returns 409 IDEMPOTENCY_KEY_REUSE")
    void testIdempotency_KeyReuseWithDifferentPayload_Conflict() throws Exception {
        String key = "key-conflict-" + UUID.randomUUID();
        EmergencyBloodRequestRequest originalRequest = buildRequest();

        // Initial submission
        mockMvc.perform(post("/api/v1/emergency/blood-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(originalRequest)))
                .andExpect(status().isCreated());

        // Second submission with same key but different units
        EmergencyBloodRequestRequest modifiedRequest = buildRequest();
        modifiedRequest.setUnitsRequired(5); // Changed from 3 to 5

        mockMvc.perform(post("/api/v1/emergency/blood-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(modifiedRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("IDEMPOTENCY_KEY_REUSE"))
                .andExpect(jsonPath("$.message", containsString("different request payload")));
    }

    @Test
    @DisplayName("Expired idempotency record is treated as a new request subject to rate limiting")
    void testIdempotency_ExpiredRecord_TreatedAsNewRequest() throws Exception {
        String key = "key-expired-" + UUID.randomUUID();
        EmergencyBloodRequestRequest request = buildRequest();

        // Initial submission
        MvcResult firstResult = mockMvc.perform(post("/api/v1/emergency/blood-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String firstId = objectMapper.readTree(firstResult.getResponse().getContentAsString()).get("id").asText();

        // Manually expire the record in the database
        IdempotencyRecord record = idempotencyRecordRepository.findByUserIdAndIdempotencyKeyAndResourceType(
                testUser.getId(), key, EmergencyIdempotencyService.RESOURCE_TYPE_BLOOD_REQUEST).orElseThrow();
        record.setExpiresAt(Instant.now().minus(2, ChronoUnit.HOURS));
        idempotencyRecordRepository.save(record);

        // Re-submit with same key: should be treated as a new request (201 Created with new ID)
        MvcResult secondResult = mockMvc.perform(post("/api/v1/emergency/blood-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String secondId = objectMapper.readTree(secondResult.getResponse().getContentAsString()).get("id").asText();

        assertNotEquals(firstId, secondId, "Expired record submission must create a new blood request");
        assertEquals(2, bloodRequestRepository.count());
    }

    @Test
    @DisplayName("Concurrent identical submissions with same key produce exactly one BloodRequest")
    void testIdempotency_ConcurrentSubmissions() throws Exception {
        String key = "concurrent-key-" + UUID.randomUUID();
        EmergencyBloodRequestRequest request = buildRequest();
        String jsonPayload = objectMapper.writeValueAsString(request);

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        List<Integer> statusCodes = Collections.synchronizedList(new ArrayList<>());
        Set<String> returnedIds = Collections.synchronizedSet(new HashSet<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    MvcResult result = mockMvc.perform(post("/api/v1/emergency/blood-requests")
                                    .header("Authorization", "Bearer " + userToken)
                                    .header("Idempotency-Key", key)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonPayload))
                            .andReturn();

                    int status = result.getResponse().getStatus();
                    statusCodes.add(status);
                    if (status == 200 || status == 201) {
                        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
                        returnedIds.add(json.get("id").asText());
                    }
                } catch (Exception e) {
                    // unexpected error
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Exactly one database record created
        assertEquals(1, bloodRequestRepository.count(), "Exactly one BloodRequest must be created in DB");

        // All successful threads must have received the same request ID
        assertEquals(1, returnedIds.size(), "All threads must receive the same request ID");

        // Exactly one 201 Created and four 200 OK
        long count201 = statusCodes.stream().filter(s -> s == 201).count();
        long count200 = statusCodes.stream().filter(s -> s == 200).count();
        assertEquals(1, count201, "Exactly one thread must receive 201 Created");
        assertEquals(threadCount - 1, count200, "All other concurrent threads must receive 200 OK replay");
    }

    @Test
    @DisplayName("Concurrent identical submissions with an EXPIRED record produce exactly one new BloodRequest, one 201, and four 200 replays")
    void testIdempotency_ConcurrentSubmissions_WithExpiredRecord() throws Exception {
        String key = "concurrent-expired-" + UUID.randomUUID();
        EmergencyBloodRequestRequest request = buildRequest();
        String jsonPayload = objectMapper.writeValueAsString(request);

        // 1. Pre-insert an expired idempotency record in the database
        IdempotencyRecord expiredRecord = new IdempotencyRecord(
                testUser.getId(),
                key,
                idempotencyService.computeFingerprint(request),
                EmergencyIdempotencyService.RESOURCE_TYPE_BLOOD_REQUEST,
                UUID.randomUUID(), // old/prior request ID
                201,
                Instant.now().minus(2, ChronoUnit.HOURS) // Expired 2 hours ago
        );
        idempotencyRecordRepository.saveAndFlush(expiredRecord);
        assertEquals(1, idempotencyRecordRepository.count());
        assertEquals(0, bloodRequestRepository.count());

        // 2. Launch 5 concurrent threads submitting the same key and payload
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        List<Integer> statusCodes = Collections.synchronizedList(new ArrayList<>());
        Set<String> returnedIds = Collections.synchronizedSet(new HashSet<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    MvcResult result = mockMvc.perform(post("/api/v1/emergency/blood-requests")
                                    .header("Authorization", "Bearer " + userToken)
                                    .header("Idempotency-Key", key)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonPayload))
                            .andReturn();

                    int status = result.getResponse().getStatus();
                    statusCodes.add(status);
                    if (status == 200 || status == 201) {
                        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
                        returnedIds.add(json.get("id").asText());
                    }
                } catch (Exception e) {
                    // unexpected error
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Exactly one database record created for BloodRequest
        assertEquals(1, bloodRequestRepository.count(), "Exactly one new BloodRequest must be created in DB");

        // Exactly one IdempotencyRecord remains in DB, and it must now be active
        assertEquals(1, idempotencyRecordRepository.count(), "Exactly one IdempotencyRecord in DB");
        IdempotencyRecord updatedRecord = idempotencyRecordRepository.findByUserIdAndIdempotencyKeyAndResourceType(
                testUser.getId(), key, EmergencyIdempotencyService.RESOURCE_TYPE_BLOOD_REQUEST).orElseThrow();
        assertFalse(updatedRecord.isExpired(), "Updated idempotency record must now be active");
        assertEquals(returnedIds.iterator().next(), updatedRecord.getResourceId().toString());

        // All threads must receive the same request ID
        assertEquals(1, returnedIds.size(), "All threads must receive the same request ID");

        // Exactly one 201 Created and four 200 OK
        long count201 = statusCodes.stream().filter(s -> s == 201).count();
        long count200 = statusCodes.stream().filter(s -> s == 200).count();
        assertEquals(1, count201, "Exactly one thread must receive 201 Created");
        assertEquals(threadCount - 1, count200, "All other concurrent threads must receive 200 OK replay");
    }

    @Test
    @DisplayName("Fingerprint verification: same logical payload produces identical fingerprint; different coordinates produce distinct fingerprints")
    void testFingerprint_PrecisionAndDeterminism() {
        EmergencyBloodRequestRequest req1 = buildRequest();
        EmergencyBloodRequestRequest req2 = buildRequest();

        String fp1 = idempotencyService.computeFingerprint(req1);
        String fp2 = idempotencyService.computeFingerprint(req2);
        assertEquals(fp1, fp2, "Identical logical requests must produce the exact same fingerprint");

        // Meaningfully different coordinates (e.g. 19.0522 vs 19.0523)
        EmergencyBloodRequestRequest reqDiffLat = buildRequest();
        reqDiffLat.setLatitude(19.0523);
        String fpDiffLat = idempotencyService.computeFingerprint(reqDiffLat);
        assertNotEquals(fp1, fpDiffLat, "Meaningfully different latitude must produce different fingerprint");

        EmergencyBloodRequestRequest reqDiffLng = buildRequest();
        reqDiffLng.setLongitude(72.8296);
        String fpDiffLng = idempotencyService.computeFingerprint(reqDiffLng);
        assertNotEquals(fp1, fpDiffLng, "Meaningfully different longitude must produce different fingerprint");
    }

    @Test
    @DisplayName("Cleanup mechanism: cleanExpiredRecords purges expired idempotency records from PostgreSQL")
    void testCleanExpiredRecords() {
        UUID userId = testUser.getId();
        // 1. Save active record
        idempotencyService.saveRecord(
                userId, "active-key", "fp1", EmergencyIdempotencyService.RESOURCE_TYPE_BLOOD_REQUEST,
                UUID.randomUUID(), 201, Duration.ofHours(24)
        );

        // 2. Save expired record
        IdempotencyRecord expired = idempotencyService.saveRecord(
                userId, "expired-key", "fp2", EmergencyIdempotencyService.RESOURCE_TYPE_BLOOD_REQUEST,
                UUID.randomUUID(), 201, Duration.ofHours(1)
        );
        expired.setExpiresAt(Instant.now().minus(2, ChronoUnit.HOURS));
        idempotencyRecordRepository.save(expired);

        assertEquals(2, idempotencyRecordRepository.count());

        // Run cleanup
        long cleaned = idempotencyService.cleanExpiredRecords();
        assertEquals(1, cleaned, "Exactly 1 expired record should be deleted");
        assertEquals(1, idempotencyRecordRepository.count());
        assertTrue(idempotencyRecordRepository.findByUserIdAndIdempotencyKeyAndResourceType(
                userId, "active-key", EmergencyIdempotencyService.RESOURCE_TYPE_BLOOD_REQUEST).isPresent());
        assertFalse(idempotencyRecordRepository.findByUserIdAndIdempotencyKeyAndResourceType(
                userId, "expired-key", EmergencyIdempotencyService.RESOURCE_TYPE_BLOOD_REQUEST).isPresent());
    }

    @Test
    @DisplayName("Security & Privacy: Raw Idempotency-Key and sensitive request details are NEVER emitted to application logs")
    void testLogPrivacy_NeverLogsRawIdempotencyKey() throws Exception {
        ch.qos.logback.classic.Logger emergencyLogger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.netra.features.emergency");
        ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> listAppender =
                new ch.qos.logback.core.read.ListAppender<>();
        listAppender.start();
        emergencyLogger.addAppender(listAppender);

        String secretKey = "RAW_IDEMP_KEY_SUPER_SECRET_TOKEN_XYZ";
        EmergencyBloodRequestRequest request = buildRequest();

        try {
            // 1. Initial submission (201 Created)
            mockMvc.perform(post("/api/v1/emergency/blood-requests")
                            .header("Authorization", "Bearer " + userToken)
                            .header("Idempotency-Key", secretKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            // 2. Replay submission (200 OK)
            mockMvc.perform(post("/api/v1/emergency/blood-requests")
                            .header("Authorization", "Bearer " + userToken)
                            .header("Idempotency-Key", secretKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            // 3. Conflict submission with modified payload (409 IDEMPOTENCY_KEY_REUSE)
            EmergencyBloodRequestRequest modifiedRequest = buildRequest();
            modifiedRequest.setUnitsRequired(9);
            mockMvc.perform(post("/api/v1/emergency/blood-requests")
                            .header("Authorization", "Bearer " + userToken)
                            .header("Idempotency-Key", secretKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(modifiedRequest)))
                    .andExpect(status().isConflict());

            // 4. Expired submission
            IdempotencyRecord record = idempotencyRecordRepository.findByUserIdAndIdempotencyKeyAndResourceType(
                    testUser.getId(), secretKey, EmergencyIdempotencyService.RESOURCE_TYPE_BLOOD_REQUEST).orElseThrow();
            record.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
            idempotencyRecordRepository.save(record);

            mockMvc.perform(post("/api/v1/emergency/blood-requests")
                            .header("Authorization", "Bearer " + userToken)
                            .header("Idempotency-Key", secretKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            // Verify captured logs
            assertFalse(listAppender.list.isEmpty(), "Expected application logs to be captured");
            for (ch.qos.logback.classic.spi.ILoggingEvent event : listAppender.list) {
                String formatted = event.getFormattedMessage();
                assertFalse(formatted.contains(secretKey),
                        "Log message must NEVER contain raw idempotency key: " + formatted);
                assertFalse(formatted.contains("Lilavati"),
                        "Log message must not contain hospital name: " + formatted);
                assertFalse(formatted.contains("O_POSITIVE"),
                        "Log message must not contain blood group: " + formatted);
                assertFalse(formatted.contains("19.0522"),
                        "Log message must not contain exact coordinates: " + formatted);
            }
        } finally {
            emergencyLogger.detachAppender(listAppender);
        }
    }

    @Test
    @DisplayName("Data Integrity Violation: Unrelated DataIntegrityViolationException is re-thrown and NOT converted to replay")
    void testUnrelatedDataIntegrityViolation_IsRethrownAndNotConvertedToReplay() {
        DataIntegrityViolationException unrelatedEx = new DataIntegrityViolationException(
                "could not execute statement; SQL [n/a]; constraint [fk_blood_request_user]; nested exception is org.hibernate.exception.ConstraintViolationException: could not execute statement"
        );

        EmergencyTransactionalService mockTxService = new EmergencyTransactionalService(
                bloodRequestService, idempotencyService, rateLimitingService, auditService
        ) {
            @Override
            public org.netra.features.emergency.dto.EmergencyCreationResult executeCreation(
                    UUID currentUserId,
                    EmergencyBloodRequestRequest request,
                    String idempotencyKey,
                    String fingerprint,
                    String windowKey,
                    String clientIp,
                    String userAgent) {
                throw unrelatedEx;
            }
        };

        EmergencyService customEmergencyService = new EmergencyService(
                bloodRequestService,
                idempotencyService,
                mockTxService,
                rateLimitingService,
                auditService
        );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser.getId(), null, Collections.emptyList())
        );

        EmergencyBloodRequestRequest request = buildRequest();
        String key = "unrelated-ex-key-" + UUID.randomUUID();

        // Must re-throw DataIntegrityViolationException directly without treating as replay
        DataIntegrityViolationException thrown = assertThrows(
                DataIntegrityViolationException.class,
                () -> customEmergencyService.createEmergencyRequest(request, key, "127.0.0.1", "TestAgent")
        );

        assertSame(unrelatedEx, thrown, "Unrelated DataIntegrityViolationException must be rethrown unchanged");
    }

    @Test
    @DisplayName("Constraint inspection: correctly distinguishes idempotency constraint from foreign key or check constraints")
    void testConstraintInspection_DistinguishesIdempotencyConstraint() {
        // 1. Unrelated FK violation -> false
        DataIntegrityViolationException fkEx = new DataIntegrityViolationException(
                "violates foreign key constraint fk_blood_request_user");
        assertFalse(emergencyService.isIdempotencyConstraintViolation(fkEx));

        // 2. Unrelated check constraint violation -> false
        DataIntegrityViolationException checkEx = new DataIntegrityViolationException(
                "violates check constraint ck_units_positive");
        assertFalse(emergencyService.isIdempotencyConstraintViolation(checkEx));

        // 3. Unrelated table unique constraint violation -> false
        DataIntegrityViolationException otherUniqueEx = new DataIntegrityViolationException(
                "duplicate key value violates unique constraint uq_users_email");
        assertFalse(emergencyService.isIdempotencyConstraintViolation(otherUniqueEx));

        // 4. Matching idempotency unique constraint violation -> true
        DataIntegrityViolationException idempEx = new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"uq_idempotency_user_key_resource\"");
        assertTrue(emergencyService.isIdempotencyConstraintViolation(idempEx));

        // 5. Nested Hibernate ConstraintViolationException -> true
        org.hibernate.exception.ConstraintViolationException hibernateCve =
                new org.hibernate.exception.ConstraintViolationException(
                        "could not execute statement",
                        new java.sql.SQLException("duplicate key", "23505"),
                        "uq_idempotency_user_key_resource"
                );
        DataIntegrityViolationException nestedEx = new DataIntegrityViolationException(
                "could not execute statement", hibernateCve);
        assertTrue(emergencyService.isIdempotencyConstraintViolation(nestedEx));
    }

    @Test
    @DisplayName("Idempotency Replay: replay still works after original emergency request requiredBy deadline has passed")
    void testIdempotentReplayStillWorksAfterOriginalDeadlinePasses() throws Exception {
        String key = "key-deadline-passed-" + UUID.randomUUID();
        // 1. Create a valid emergency request with a short future requiredBy
        Instant shortDeadline = Instant.now().plus(60, ChronoUnit.MILLIS);
        EmergencyBloodRequestRequest request = new EmergencyBloodRequestRequest(
                BloodGroup.O_POSITIVE,
                2,
                "Lilavati Hospital",
                "A-791, Bandra Reclamation",
                "Mumbai",
                "Maharashtra",
                "400050",
                19.0522,
                72.8295,
                shortDeadline,
                "Urgent requirement for emergency surgery."
        );

        MvcResult firstResult = mockMvc.perform(post("/api/v1/emergency/blood-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String originalRequestId = objectMapper.readTree(firstResult.getResponse().getContentAsString()).get("id").asText();
        assertEquals(1, bloodRequestRepository.count());

        long initialAuditCount = securityAuditLogRepository.findAll().stream()
                .filter(l -> "EMERGENCY_REQUEST_CREATED".equals(l.getEventType()))
                .count();
        assertEquals(1, initialAuditCount);

        // 3. Move/test beyond requiredBy without arbitrary long delays
        while (!Instant.now().isAfter(shortDeadline)) {
            Thread.sleep(10);
        }
        assertTrue(Instant.now().isAfter(shortDeadline), "Current time must now be strictly past the request deadline");

        // 4. Submit the exact same payload with the same idempotency key
        // 5. Expect HTTP 200
        MvcResult replayResult = mockMvc.perform(post("/api/v1/emergency/blood-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // 6. Verify the returned request ID equals the original
        String replayRequestId = objectMapper.readTree(replayResult.getResponse().getContentAsString()).get("id").asText();
        assertEquals(originalRequestId, replayRequestId);

        // 7. Verify BloodRequest count remains unchanged
        assertEquals(1, bloodRequestRepository.count());

        // 8. Verify no additional EMERGENCY_REQUEST_CREATED audit event
        long replayAuditCount = securityAuditLogRepository.findAll().stream()
                .filter(l -> "EMERGENCY_REQUEST_CREATED".equals(l.getEventType()))
                .count();
        assertEquals(1, replayAuditCount, "No additional EMERGENCY_REQUEST_CREATED audit event should be logged");

        // 9. Verify no additional rate-limit consumption (user can still make all 4 remaining requests)
        for (int i = 1; i <= 4; i++) {
            EmergencyBloodRequestRequest req = buildRequest();
            req.setHospitalName("Remaining Quota Hospital " + i);
            mockMvc.perform(post("/api/v1/emergency/blood-requests")
                            .header("Authorization", "Bearer " + userToken)
                            .header("Idempotency-Key", "subsequent-quota-key-" + i)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());
        }
    }
}
