package org.netra.features.remediation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.core.audit.SecurityAuditLog;
import org.netra.core.audit.SecurityAuditLogRepository;
import org.netra.core.exception.TokenReuseException;
import org.netra.core.security.JwtTokenProvider;
import org.netra.features.auth.dto.TokenRefreshRequest;
import org.netra.features.auth.service.AuthService;
import org.netra.features.bloodbank.entity.*;
import org.netra.features.bloodbank.repository.BloodBankAccountRepository;
import org.netra.features.bloodbank.repository.BloodBankRepository;
import org.netra.features.donor.dto.CreateDonorProfileRequest;
import org.netra.features.donor.dto.VerifyDonorBloodGroupRequest;
import org.netra.features.donor.entity.*;
import org.netra.features.donor.repository.DonorProfileRepository;
import org.netra.features.events.dto.UpdateDonationEventRequest;
import org.netra.features.events.entity.DonationEvent;
import org.netra.features.events.entity.DonationEventStatus;
import org.netra.features.events.repository.DonationEventRepository;
import org.netra.features.notification.dto.RegisterDeviceTokenRequest;
import org.netra.features.notification.dto.RevokeDeviceTokenRequest;
import org.netra.features.notification.entity.DevicePlatform;
import org.netra.features.notification.entity.UserDeviceToken;
import org.netra.features.notification.repository.UserDeviceTokenRepository;
import org.netra.features.user.entity.RefreshSession;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.entity.UserStatus;
import org.netra.features.user.repository.RefreshSessionRepository;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RemediationVerificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonorProfileRepository donorProfileRepository;

    @Autowired
    private BloodBankRepository bloodBankRepository;

    @Autowired
    private BloodBankAccountRepository bloodBankAccountRepository;

    @Autowired
    private RefreshSessionRepository refreshSessionRepository;

    @Autowired
    private UserDeviceTokenRepository userDeviceTokenRepository;

    @Autowired
    private DonationEventRepository donationEventRepository;

    @Autowired
    private SecurityAuditLogRepository securityAuditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private AuthService authService;

    private User createTestUser(String namePrefix, UserRole role) {
        User user = new User(
                namePrefix + " User",
                namePrefix.toLowerCase() + "." + UUID.randomUUID() + "@netra.org",
                "+919876543210",
                passwordEncoder.encode("SecurePass123"),
                Set.of(role)
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

    private BloodBank createTestBloodBank(String name, BloodBankVerificationStatus status) {
        BloodBank bank = new BloodBank();
        bank.setName(name);
        bank.setAddress("100 Cross Rd");
        bank.setCity("Mumbai");
        bank.setState("Maharashtra");
        bank.setPostalCode("400001");
        bank.setLatitude(18.9400);
        bank.setLongitude(72.8350);
        bank.setPhone("+912224177111");
        bank.setEmail(name.toLowerCase().replaceAll("[^a-z0-9]", "") + "@netra.org");
        bank.setOperatingStatus(BloodBankOperatingStatus.OPEN);
        bank.setVerificationStatus(status);
        return bloodBankRepository.save(bank);
    }

    // =========================================================================
    // Defect 1: Biological Sex Plumbing & Validation
    // =========================================================================

    @Test
    @DisplayName("Remediation 1: Biological sex is persisted, retrieved, and validated")
    void testBiologicalSexPlumbingAndValidation() throws Exception {
        User femaleUser = createTestUser("SexTestFemale", UserRole.ROLE_DONOR);
        String token = getAccessToken(femaleUser);

        CreateDonorProfileRequest validReq = new CreateDonorProfileRequest(
                BloodGroup.O_POSITIVE,
                DonorAvailabilityStatus.AVAILABLE,
                18.9400,
                72.8350,
                "FEMALE"
        );

        mockMvc.perform(post("/api/v1/donor/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.biologicalSex", is("FEMALE")))
                .andExpect(jsonPath("$.bloodGroup", is("O+")));

        // Verify retrieval
        mockMvc.perform(get("/api/v1/donor/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biologicalSex", is("FEMALE")));

        // Invalid biological sex rejected
        User invalidUser = createTestUser("SexTestInvalid", UserRole.ROLE_DONOR);
        String invalidToken = getAccessToken(invalidUser);

        String invalidPayload = "{\"bloodGroup\":\"O+\",\"availabilityStatus\":\"AVAILABLE\",\"biologicalSex\":\"INVALID_SEX\",\"latitude\":18.94,\"longitude\":72.83}";
        mockMvc.perform(post("/api/v1/donor/profile")
                        .header("Authorization", "Bearer " + invalidToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // Defect 2: Blood Group Verification Workflow & Anti-Fraud
    // =========================================================================

    @Test
    @DisplayName("Remediation 2: Blood group verification workflow, RBAC, and self-verification rejection")
    void testBloodGroupVerificationWorkflow() throws Exception {
        User donorUser = createTestUser("DonorVerifyTarget", UserRole.ROLE_DONOR);
        DonorProfile dp = new DonorProfile(donorUser.getId(), BloodGroup.B_POSITIVE, DonorAvailabilityStatus.AVAILABLE);
        dp.setBloodGroupVerificationStatus(BloodGroupVerificationStatus.SELF_REPORTED);
        donorProfileRepository.save(dp);

        User adminUser = createTestUser("AdminVerifier", UserRole.ROLE_ADMIN);
        String adminToken = getAccessToken(adminUser);

        User bloodbankStaff = createTestUser("BBStaffVerifier", UserRole.ROLE_BLOODBANK);
        String staffToken = getAccessToken(bloodbankStaff);

        BloodBank bank = createTestBloodBank("Verification City Bank", BloodBankVerificationStatus.VERIFIED);

        // 1. Staff without active account is rejected -> 403
        VerifyDonorBloodGroupRequest verifyReq = new VerifyDonorBloodGroupRequest("Verified against hospital lab report");
        mockMvc.perform(post("/api/v1/donors/" + donorUser.getId() + "/verify-blood-group")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isForbidden());

        // 2. Link staff with ACTIVE account -> 200 OK
        BloodBankAccount account = new BloodBankAccount(bloodbankStaff.getId(), bank.getId(), BloodBankAccountStatus.ACTIVE);
        bloodBankAccountRepository.save(account);

        mockMvc.perform(post("/api/v1/donors/" + donorUser.getId() + "/verify-blood-group")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bloodGroupVerificationStatus", is("VERIFIED")))
                .andExpect(jsonPath("$.verifiedBy", is(bloodbankStaff.getId().toString())))
                .andExpect(jsonPath("$.verificationNotes", is("Verified against hospital lab report")))
                .andExpect(jsonPath("$.verifiedAt", notNullValue()));

        // 3. Self-verification anti-fraud: Admin cannot verify own donor profile -> 400
        DonorProfile adminDp = new DonorProfile(adminUser.getId(), BloodGroup.A_POSITIVE, DonorAvailabilityStatus.AVAILABLE);
        adminDp.setBloodGroupVerificationStatus(BloodGroupVerificationStatus.SELF_REPORTED);
        donorProfileRepository.save(adminDp);

        mockMvc.perform(post("/api/v1/donors/" + adminUser.getId() + "/verify-blood-group")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot verify their own blood group")));

        // 4. Regular donor cannot verify another donor -> 403
        User anotherDonor = createTestUser("UnauthorizedDonor", UserRole.ROLE_DONOR);
        String donorToken = getAccessToken(anotherDonor);
        mockMvc.perform(post("/api/v1/donors/" + donorUser.getId() + "/verify-blood-group")
                        .header("Authorization", "Bearer " + donorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // Defect 5: Refresh Token CAS and Concurrency Race Condition Protection
    // =========================================================================

    @Test
    @DisplayName("Remediation 5: Concurrent refresh requests trigger atomic CAS and TokenReuseException")
    void testConcurrentRefreshTokenAtomicCas() throws Exception {
        User user = createTestUser("CasRaceUser", UserRole.ROLE_DONOR);
        String rawRefreshToken = "test-raw-refresh-token-" + UUID.randomUUID();
        String tokenHash = org.netra.core.security.SecurityUtils.sha256Hex(rawRefreshToken);
        UUID familyId = UUID.randomUUID();

        RefreshSession session = new RefreshSession(
                user,
                tokenHash,
                familyId,
                null,
                Instant.now().plus(7, ChronoUnit.DAYS),
                "127.0.0.1",
                "JUnit",
                "test-device"
        );
        refreshSessionRepository.save(session);

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CyclicBarrier barrier = new CyclicBarrier(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger reuseCount = new AtomicInteger(0);

        List<Callable<Void>> tasks = List.of(
                () -> {
                    barrier.await(5, TimeUnit.SECONDS);
                    try {
                        authService.refresh(new TokenRefreshRequest(rawRefreshToken), "127.0.0.1", "JUnit-1");
                        successCount.incrementAndGet();
                    } catch (TokenReuseException e) {
                        reuseCount.incrementAndGet();
                    }
                    return null;
                },
                () -> {
                    barrier.await(5, TimeUnit.SECONDS);
                    try {
                        authService.refresh(new TokenRefreshRequest(rawRefreshToken), "127.0.0.1", "JUnit-2");
                        successCount.incrementAndGet();
                    } catch (TokenReuseException e) {
                        reuseCount.incrementAndGet();
                    }
                    return null;
                }
        );

        List<Future<Void>> futures = executor.invokeAll(tasks);
        for (Future<Void> f : futures) {
            f.get();
        }
        executor.shutdown();

        // Exactly one refresh succeeds, the other is caught by atomic CAS as reuse
        assertEquals(1, successCount.get(), "Exactly one concurrent refresh must succeed");
        assertEquals(1, reuseCount.get(), "The second concurrent refresh must trigger TokenReuseException");

        // The entire family should now be revoked
        List<RefreshSession> sessions = refreshSessionRepository.findByFamilyId(familyId);
        for (RefreshSession s : sessions) {
            assertTrue(s.isRevoked(), "All sessions in the family must be revoked following reuse detection");
        }
    }

    // =========================================================================
    // Defect 7: Blood Bank Visibility & Unverified Access Security
    // =========================================================================

    @Test
    @DisplayName("Remediation 7: Unverified blood bank and its inventory return 404 to non-staff")
    void testUnverifiedBloodBankVisibility() throws Exception {
        BloodBank unverifiedBank = createTestBloodBank("Unverified Secret Bank", BloodBankVerificationStatus.PENDING);

        User donor = createTestUser("PublicDonorBankTester", UserRole.ROLE_DONOR);
        String donorToken = getAccessToken(donor);

        // 1. Donor accessing unverified bank -> 404
        mockMvc.perform(get("/api/v1/bloodbanks/" + unverifiedBank.getId())
                        .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isNotFound());

        // 2. Public accessing unverified bank inventory -> 404
        mockMvc.perform(get("/api/v1/bloodbanks/" + unverifiedBank.getId() + "/inventory"))
                .andExpect(status().isNotFound());

        // 3. Admin accessing unverified bank -> 200 OK
        User admin = createTestUser("AdminBankInspector", UserRole.ROLE_ADMIN);
        String adminToken = getAccessToken(admin);
        mockMvc.perform(get("/api/v1/bloodbanks/" + unverifiedBank.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Unverified Secret Bank")));
    }

    // =========================================================================
    // Defect 8: Donation Event Coordinate Pair Validation
    // =========================================================================

    @Test
    @DisplayName("Remediation 8: DonationEvent update enforces latitude and longitude as strict coordinate pair")
    void testDonationEventCoordinatePairValidation() throws Exception {
        User admin = createTestUser("AdminEventUpdater", UserRole.ROLE_ADMIN);
        String adminToken = getAccessToken(admin);

        BloodBank bank = createTestBloodBank("Event Host Bank", BloodBankVerificationStatus.VERIFIED);

        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant end = start.plus(8, ChronoUnit.HOURS);

        DonationEvent event = new DonationEvent();
        event.setBloodBankId(bank.getId());
        event.setTitle("Mega Donation Camp");
        event.setVenueName("Community Hall");
        event.setAddress("50 Clinic St");
        event.setCity("Pune");
        event.setState("Maharashtra");
        event.setPostalCode("411001");
        event.setStartAt(start);
        event.setEndAt(end);
        event.setRegistrationOpenAt(start.minus(1, ChronoUnit.DAYS));
        event.setRegistrationCloseAt(start);
        event.setDonorCapacity(100);
        event.setCreatedBy(admin.getId());
        event.setStatus(DonationEventStatus.PUBLISHED);
        event.setLatitude(18.5204);
        event.setLongitude(73.8567);
        event = donationEventRepository.save(event);

        // 1. Update latitude only -> 400
        UpdateDonationEventRequest latOnly = new UpdateDonationEventRequest();
        latOnly.setLatitude(18.5300);
        mockMvc.perform(put("/api/v1/donation-events/" + event.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(latOnly)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Latitude and longitude must either both be supplied or both be omitted")));

        // 2. Update longitude only -> 400
        UpdateDonationEventRequest lngOnly = new UpdateDonationEventRequest();
        lngOnly.setLongitude(73.8600);
        mockMvc.perform(put("/api/v1/donation-events/" + event.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lngOnly)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Latitude and longitude must either both be supplied or both be omitted")));

        // 3. Update with both coordinates -> 200 OK
        UpdateDonationEventRequest pairReq = new UpdateDonationEventRequest();
        pairReq.setLatitude(18.5300);
        pairReq.setLongitude(73.8600);
        mockMvc.perform(put("/api/v1/donation-events/" + event.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pairReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude", is(18.5300)))
                .andExpect(jsonPath("$.longitude", is(73.8600)));
    }

    // =========================================================================
    // Defect 11: FCM Device Token Revocation by Raw String
    // =========================================================================

    @Test
    @DisplayName("Remediation 11: Device token revocation by raw token string with IDOR protection")
    void testDeviceTokenRevocationByString() throws Exception {
        User userA = createTestUser("UserDeviceA", UserRole.ROLE_DONOR);
        String tokenA = getAccessToken(userA);

        User userB = createTestUser("UserDeviceB", UserRole.ROLE_DONOR);
        String tokenB = getAccessToken(userB);

        String rawDeviceTokenA = "fcm-device-token-userA-" + UUID.randomUUID();

        // 1. User A registers token
        RegisterDeviceTokenRequest regReq = new RegisterDeviceTokenRequest(
                rawDeviceTokenA, DevicePlatform.ANDROID, "FCM"
        );
        mockMvc.perform(post("/api/v1/devices/tokens")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active", is(true)));

        // 2. User B tries to revoke User A's token -> 403 Forbidden (IDOR)
        RevokeDeviceTokenRequest revokeReq = new RevokeDeviceTokenRequest(rawDeviceTokenA);
        mockMvc.perform(post("/api/v1/devices/tokens/revoke")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revokeReq)))
                .andExpect(status().isForbidden());

        // 3. User A revokes own token -> 204 No Content
        mockMvc.perform(post("/api/v1/devices/tokens/revoke")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revokeReq)))
                .andExpect(status().isNoContent());

        // Verify token is marked inactive in DB
        String hash = org.netra.core.security.SecurityUtils.sha256Hex(rawDeviceTokenA);
        UserDeviceToken tokenInDb = userDeviceTokenRepository.findByTokenHash(hash).orElseThrow();
        assertFalse(tokenInDb.isActive());
        assertNotNull(tokenInDb.getRevokedAt());
    }
}
