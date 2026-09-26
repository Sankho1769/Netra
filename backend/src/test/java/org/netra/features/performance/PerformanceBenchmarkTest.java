package org.netra.features.performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.netra.core.ratelimit.RateLimitingService;
import org.netra.core.security.JwtTokenProvider;
import org.netra.features.auth.dto.LoginRequest;
import org.netra.features.bloodbank.entity.BloodBank;
import org.netra.features.bloodbank.entity.BloodBankAccount;
import org.netra.features.bloodbank.entity.BloodBankAccountStatus;
import org.netra.features.bloodbank.entity.BloodBankOperatingStatus;
import org.netra.features.bloodbank.entity.BloodBankVerificationStatus;
import org.netra.features.bloodbank.repository.BloodBankAccountRepository;
import org.netra.features.bloodbank.repository.BloodBankRepository;
import org.netra.features.bloodrequest.dto.CreateBloodRequestRequest;
import org.netra.features.bloodrequest.entity.BloodRequest;
import org.netra.features.bloodrequest.entity.BloodRequestStatus;
import org.netra.features.bloodrequest.entity.BloodRequestUrgency;
import org.netra.features.bloodrequest.repository.BloodRequestRepository;
import org.netra.features.donation.dto.CreateDonationClaimRequest;
import org.netra.features.donation.dto.VerifyDonationRequest;
import org.netra.features.donation.entity.Donation;
import org.netra.features.donation.entity.DonationSourceType;
import org.netra.features.donation.entity.DonationVerificationStatus;
import org.netra.features.donation.repository.DonationRepository;
import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.donor.entity.BloodGroupVerificationStatus;
import org.netra.features.donor.entity.DonorAvailabilityStatus;
import org.netra.features.donor.entity.DonorProfile;
import org.netra.features.donor.entity.DonorStatus;
import org.netra.features.donor.repository.DonorProfileRepository;
import org.netra.features.emergency.dto.EmergencyBloodRequestRequest;
import org.netra.features.fulfillment.dto.CreateFulfillmentRequest;
import org.netra.features.fulfillment.entity.Fulfillment;
import org.netra.features.fulfillment.repository.FulfillmentRepository;
import org.netra.features.matching.entity.DonorMatch;
import org.netra.features.matching.repository.DonorMatchRepository;
import org.netra.features.notification.entity.Notification;
import org.netra.features.notification.entity.NotificationReferenceType;
import org.netra.features.notification.entity.NotificationType;
import org.netra.features.notification.repository.NotificationRepository;
import org.netra.features.user.entity.RefreshSession;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.entity.UserStatus;
import org.netra.features.user.repository.RefreshSessionRepository;
import org.netra.features.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 1 — Performance Baseline Benchmark Test.
 *
 * Measures HTTP API baseline latency and throughput using real MockMvc HTTP round-trips
 * across the 11 key operational flows:
 * 1. Authentication / Login
 * 2. Blood Request Creation
 * 3. Blood Request Listing
 * 4. Emergency Request Creation
 * 5. Donor Matching
 * 6. Donor Response
 * 7. Notification Retrieval
 * 8. Donation Claim
 * 9. Donation Verification
 * 10. Fulfillment Creation
 * 11. Fulfillment Completion
 *
 * Adheres strictly to methodology:
 * - Warm-up iterations before measurement
 * - 500 measured iterations for lightweight endpoints
 * - Justified sample size for multi-step workflows
 * - Reports p50, p95, p99, min, max, throughput, and error rate
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PerformanceBenchmarkTest {

    private static final Logger log = LoggerFactory.getLogger(PerformanceBenchmarkTest.class);

    private static final List<BenchmarkResult> BENCHMARK_RESULTS = new ArrayList<>();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshSessionRepository refreshSessionRepository;

    @Autowired
    private DonorProfileRepository donorProfileRepository;

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private BloodBankRepository bloodBankRepository;

    @Autowired
    private BloodBankAccountRepository bloodBankAccountRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private DonorMatchRepository donorMatchRepository;

    @Autowired
    private FulfillmentRepository fulfillmentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RateLimitingService rateLimitingService;

    private User primaryDonor;
    private User primaryReceiver;
    private User bankStaff;
    private User adminUser;
    private BloodBank bloodBank;

    private String donorToken;
    private String receiverToken;
    private String staffToken;

    @BeforeEach
    void setUp() {
        rateLimitingService.reset();
        setupBaselineUsers();
    }

    private void setupBaselineUsers() {
        if (primaryDonor != null) return;

        primaryDonor = createUser("Baseline Donor", "perf.donor@netra.org", Set.of(UserRole.ROLE_DONOR));
        primaryReceiver = createUser("Baseline Receiver", "perf.receiver@netra.org", Set.of(UserRole.ROLE_RECEIVER));
        bankStaff = createUser("Baseline Staff", "perf.staff@netra.org", Set.of(UserRole.ROLE_BLOODBANK));
        adminUser = createUser("Baseline Admin", "perf.admin@netra.org", Set.of(UserRole.ROLE_ADMIN));

        donorToken = tokenFor(primaryDonor);
        receiverToken = tokenFor(primaryReceiver);
        staffToken = tokenFor(bankStaff);

        bloodBank = bloodBankRepository.findAll().stream().findFirst().orElseGet(() -> {
            BloodBank bb = new BloodBank("Metro Hospital Blood Bank", "REG-PERF-01", "100 Health Blvd",
                    "Mumbai", "Maharashtra", "400001", 19.0760, 72.8777, "+912212345678",
                    "bank.perf@netra.org", BloodBankVerificationStatus.VERIFIED, BloodBankOperatingStatus.OPEN);
            return bloodBankRepository.save(bb);
        });

        if (!bloodBankAccountRepository.existsByUserIdAndBloodBankIdAndStatus(bankStaff.getId(), bloodBank.getId(), BloodBankAccountStatus.ACTIVE)) {
            BloodBankAccount account = new BloodBankAccount(bankStaff.getId(), bloodBank.getId(), BloodBankAccountStatus.ACTIVE);
            bloodBankAccountRepository.save(account);
        }

        if (donorProfileRepository.findByUserId(primaryDonor.getId()).isEmpty()) {
            DonorProfile dp = new DonorProfile(primaryDonor.getId(), BloodGroup.O_POSITIVE, DonorAvailabilityStatus.AVAILABLE);
            dp.setBloodGroupVerificationStatus(BloodGroupVerificationStatus.VERIFIED);
            dp.setLastDonationDate(LocalDate.now().minusDays(120));
            dp.setLatitude(19.0760);
            dp.setLongitude(72.8777);
            donorProfileRepository.save(dp);
        }
    }

    private User createUser(String name, String email, Set<UserRole> roles) {
        return userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            String uniquePhone = "+919" + String.format("%09d", Math.abs((long) email.hashCode()) % 1_000_000_000L);
            User u = new User(name, email.toLowerCase(), uniquePhone, passwordEncoder.encode("SecretPass123"), roles);
            u.setStatus(UserStatus.ACTIVE);
            return userRepository.save(u);
        });
    }

    private String tokenFor(User user) {
        Set<String> roleNames = user.getRoles().stream().map(Enum::name).collect(Collectors.toSet());
        return "Bearer " + jwtTokenProvider.generateAccessToken(user.getId(), roleNames);
    }

    // =========================================================================
    // 1. AUTHENTICATION / LOGIN (500 measured, 50 warm-up)
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("Benchmark 1: Authentication / Login HTTP Latency & Throughput")
    void benchmark01_AuthenticationLogin() throws Exception {
        LoginRequest req = new LoginRequest("perf.donor@netra.org", "SecretPass123", "device-benchmark-1");
        String payload = objectMapper.writeValueAsString(req);

        // Warm-up
        for (int i = 0; i < 50; i++) {
            rateLimitingService.reset();
            mockMvc.perform(post("/api/v1/auth/login")
                    .header("X-Forwarded-For", "10.0." + (i / 15) + "." + (i % 250))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                    .andExpect(status().isOk());
        }

        // Measurement (500 iterations)
        int iterations = 500;
        List<Long> latencies = new ArrayList<>(iterations);
        int errors = 0;
        long startTotal = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            rateLimitingService.reset();
            long t0 = System.nanoTime();
            try {
                MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", "10.0." + (i / 15) + "." + (i % 250))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                        .andReturn();
                long t1 = System.nanoTime();
                latencies.add(t1 - t0);
                if (res.getResponse().getStatus() != 200) errors++;
            } catch (Exception e) {
                errors++;
            }
        }
        long durationTotal = System.nanoTime() - startTotal;
        BenchmarkResult result = new BenchmarkResult("1. Authentication / Login", latencies, errors, durationTotal / 1_000_000_000.0);
        BENCHMARK_RESULTS.add(result);
        log.info("BENCHMARK RESULT: {}", result);
        assertTrue(result.getErrorRate() == 0.0, "Login error rate must be 0%");
    }

    // =========================================================================
    // 2. BLOOD REQUEST CREATION (100 measured, 20 warm-up)
    // =========================================================================

    @Test
    @Order(2)
    @DisplayName("Benchmark 2: Blood Request Creation HTTP Latency & Throughput")
    void benchmark02_BloodRequestCreation() throws Exception {
        CreateBloodRequestRequest req = new CreateBloodRequestRequest();
        req.setBloodGroup(BloodGroup.O_POSITIVE);
        req.setUnitsRequired(2);
        req.setHospitalName("City General Hospital");
        req.setHospitalAddress("100 Central Road");
        req.setCity("Mumbai");
        req.setState("Maharashtra");
        req.setPostalCode("400001");
        req.setLatitude(19.0760);
        req.setLongitude(72.8777);
        req.setRequiredBy(Instant.now().plus(48, ChronoUnit.HOURS));

        String payload = objectMapper.writeValueAsString(req);

        // Warm-up
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(post("/api/v1/blood-requests")
                    .header("Authorization", receiverToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                    .andExpect(status().isCreated());
        }

        int iterations = 100;
        List<Long> latencies = new ArrayList<>(iterations);
        int errors = 0;
        long startTotal = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            long t0 = System.nanoTime();
            try {
                MvcResult res = mockMvc.perform(post("/api/v1/blood-requests")
                        .header("Authorization", receiverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                        .andReturn();
                long t1 = System.nanoTime();
                latencies.add(t1 - t0);
                if (res.getResponse().getStatus() != 201) errors++;
            } catch (Exception e) {
                errors++;
            }
        }
        long durationTotal = System.nanoTime() - startTotal;
        BenchmarkResult result = new BenchmarkResult("2. Blood Request Creation", latencies, errors, durationTotal / 1_000_000_000.0);
        BENCHMARK_RESULTS.add(result);
        log.info("BENCHMARK RESULT: {}", result);
        assertTrue(result.getErrorRate() == 0.0);
    }

    // =========================================================================
    // 3. BLOOD REQUEST LISTING (500 measured, 50 warm-up)
    // =========================================================================

    @Test
    @Order(3)
    @DisplayName("Benchmark 3: Blood Request Listing HTTP Latency & Throughput")
    void benchmark03_BloodRequestListing() throws Exception {
        // Warm-up
        for (int i = 0; i < 50; i++) {
            mockMvc.perform(get("/api/v1/blood-requests?page=0&size=20"))
                    .andExpect(status().isOk());
        }

        int iterations = 500;
        List<Long> latencies = new ArrayList<>(iterations);
        int errors = 0;
        long startTotal = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            long t0 = System.nanoTime();
            try {
                MvcResult res = mockMvc.perform(get("/api/v1/blood-requests?page=0&size=20"))
                        .andReturn();
                long t1 = System.nanoTime();
                latencies.add(t1 - t0);
                if (res.getResponse().getStatus() != 200) errors++;
            } catch (Exception e) {
                errors++;
            }
        }
        long durationTotal = System.nanoTime() - startTotal;
        BenchmarkResult result = new BenchmarkResult("3. Blood Request Listing", latencies, errors, durationTotal / 1_000_000_000.0);
        BENCHMARK_RESULTS.add(result);
        log.info("BENCHMARK RESULT: {}", result);
        assertTrue(result.getErrorRate() == 0.0);
    }

    // =========================================================================
    // 4. EMERGENCY REQUEST CREATION (100 measured, 20 warm-up)
    // =========================================================================

    @Test
    @Order(4)
    @DisplayName("Benchmark 4: Emergency Request Creation HTTP Latency & Throughput")
    void benchmark04_EmergencyRequestCreation() throws Exception {
        EmergencyBloodRequestRequest req = new EmergencyBloodRequestRequest(
                BloodGroup.O_POSITIVE, 2, "Trauma Care Hospital", "500 Highway Road",
                "Mumbai", "Maharashtra", "400001", 19.0760, 72.8777,
                Instant.now().plus(12, ChronoUnit.HOURS), "Critical trauma emergency"
        );
        String payload = objectMapper.writeValueAsString(req);

        // Warm-up
        for (int i = 0; i < 20; i++) {
            rateLimitingService.reset();
            mockMvc.perform(post("/api/v1/emergency/blood-requests")
                    .header("Authorization", receiverToken)
                    .header("Idempotency-Key", "warmup-idem-" + UUID.randomUUID())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                    .andExpect(status().isCreated());
        }

        int iterations = 100;
        List<Long> latencies = new ArrayList<>(iterations);
        int errors = 0;
        long startTotal = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            rateLimitingService.reset();
            long t0 = System.nanoTime();
            try {
                MvcResult res = mockMvc.perform(post("/api/v1/emergency/blood-requests")
                        .header("Authorization", receiverToken)
                        .header("Idempotency-Key", "bench-idem-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                        .andReturn();
                long t1 = System.nanoTime();
                latencies.add(t1 - t0);
                if (res.getResponse().getStatus() != 201) errors++;
            } catch (Exception e) {
                errors++;
            }
        }
        long durationTotal = System.nanoTime() - startTotal;
        BenchmarkResult result = new BenchmarkResult("4. Emergency Request Creation", latencies, errors, durationTotal / 1_000_000_000.0);
        BENCHMARK_RESULTS.add(result);
        log.info("BENCHMARK RESULT: {}", result);
        assertTrue(result.getErrorRate() == 0.0);
    }

    // =========================================================================
    // 5. DONOR MATCHING (50 measured, 10 warm-up)
    // =========================================================================

    @Test
    @Order(5)
    @DisplayName("Benchmark 5: Donor Matching Candidate Search HTTP Latency & Throughput")
    void benchmark05_DonorMatching() throws Exception {
        BloodRequest targetReq = createTestRequest(primaryReceiver.getId(), "Matching Base Hospital");

        // Warm-up
        for (int i = 0; i < 10; i++) {
            rateLimitingService.reset();
            mockMvc.perform(get("/api/v1/blood-requests/" + targetReq.getId() + "/matches")
                    .header("Authorization", receiverToken))
                    .andExpect(status().isOk());
        }

        int iterations = 50;
        List<Long> latencies = new ArrayList<>(iterations);
        int errors = 0;
        long startTotal = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            rateLimitingService.reset();
            long t0 = System.nanoTime();
            try {
                MvcResult res = mockMvc.perform(get("/api/v1/blood-requests/" + targetReq.getId() + "/matches")
                        .header("Authorization", receiverToken))
                        .andReturn();
                long t1 = System.nanoTime();
                latencies.add(t1 - t0);
                if (res.getResponse().getStatus() != 200) errors++;
            } catch (Exception e) {
                errors++;
            }
        }
        long durationTotal = System.nanoTime() - startTotal;
        BenchmarkResult result = new BenchmarkResult("5. Donor Matching Search", latencies, errors, durationTotal / 1_000_000_000.0);
        BENCHMARK_RESULTS.add(result);
        log.info("BENCHMARK RESULT: {}", result);
        assertTrue(result.getErrorRate() == 0.0);
    }

    // =========================================================================
    // 6. DONOR RESPONSE (50 measured, 10 warm-up)
    // =========================================================================

    @Test
    @Order(6)
    @DisplayName("Benchmark 6: Donor Match Response HTTP Latency & Throughput")
    void benchmark06_DonorResponse() throws Exception {
        BloodRequest targetReq = createTestRequest(primaryReceiver.getId(), "Response Hospital");

        // Pre-create 60 distinct donors & matches (10 warmup, 50 measured)
        List<DonorMatch> testMatches = new ArrayList<>();
        List<String> donorTokens = new ArrayList<>();

        for (int i = 0; i < 60; i++) {
            User donor = createUser("RespDonor" + i, "resp.donor." + i + "@netra.org", Set.of(UserRole.ROLE_DONOR));
            donorTokens.add(tokenFor(donor));
            DonorMatch dm = new DonorMatch(targetReq.getId(), donor.getId(), Instant.now(), Instant.now().plus(24, ChronoUnit.HOURS));
            testMatches.add(donorMatchRepository.save(dm));
        }

        // Warm-up (10)
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/donor/matches/" + testMatches.get(i).getId() + "/accept")
                    .header("Authorization", donorTokens.get(i)))
                    .andExpect(status().isOk());
        }

        int iterations = 50;
        List<Long> latencies = new ArrayList<>(iterations);
        int errors = 0;
        long startTotal = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            int idx = i + 10;
            long t0 = System.nanoTime();
            try {
                MvcResult res = mockMvc.perform(post("/api/v1/donor/matches/" + testMatches.get(idx).getId() + "/accept")
                        .header("Authorization", donorTokens.get(idx)))
                        .andReturn();
                long t1 = System.nanoTime();
                latencies.add(t1 - t0);
                if (res.getResponse().getStatus() != 200) errors++;
            } catch (Exception e) {
                errors++;
            }
        }
        long durationTotal = System.nanoTime() - startTotal;
        BenchmarkResult result = new BenchmarkResult("6. Donor Match Response (Accept)", latencies, errors, durationTotal / 1_000_000_000.0);
        BENCHMARK_RESULTS.add(result);
        log.info("BENCHMARK RESULT: {}", result);
        assertTrue(result.getErrorRate() == 0.0);
    }

    // =========================================================================
    // 7. NOTIFICATION RETRIEVAL (500 measured, 50 warm-up)
    // =========================================================================

    @Test
    @Order(7)
    @DisplayName("Benchmark 7: Notification Retrieval HTTP Latency & Throughput")
    void benchmark07_NotificationRetrieval() throws Exception {
        // Seed 10 sample notifications for primaryDonor
        for (int i = 0; i < 10; i++) {
            Notification n = new Notification(primaryDonor.getId(), NotificationType.MATCH_CREATED,
                    "Match Notification " + i, "Body " + i, NotificationReferenceType.DONOR_MATCH,
                    UUID.randomUUID(), "notif-seed-" + UUID.randomUUID(), Instant.now());
            notificationRepository.save(n);
        }

        // Warm-up
        for (int i = 0; i < 50; i++) {
            mockMvc.perform(get("/api/v1/notifications?page=0&size=20")
                    .header("Authorization", donorToken))
                    .andExpect(status().isOk());
        }

        int iterations = 500;
        List<Long> latencies = new ArrayList<>(iterations);
        int errors = 0;
        long startTotal = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            long t0 = System.nanoTime();
            try {
                MvcResult res = mockMvc.perform(get("/api/v1/notifications?page=0&size=20")
                        .header("Authorization", donorToken))
                        .andReturn();
                long t1 = System.nanoTime();
                latencies.add(t1 - t0);
                if (res.getResponse().getStatus() != 200) errors++;
            } catch (Exception e) {
                errors++;
            }
        }
        long durationTotal = System.nanoTime() - startTotal;
        BenchmarkResult result = new BenchmarkResult("7. Notification Retrieval", latencies, errors, durationTotal / 1_000_000_000.0);
        BENCHMARK_RESULTS.add(result);
        log.info("BENCHMARK RESULT: {}", result);
        assertTrue(result.getErrorRate() == 0.0);
    }

    // =========================================================================
    // 8. DONATION CLAIM (50 measured, 10 warm-up)
    // =========================================================================

    @Test
    @Order(8)
    @DisplayName("Benchmark 8: Donation Claim Submission HTTP Latency & Throughput")
    void benchmark08_DonationClaim() throws Exception {
        // Prepare 60 requests for 60 distinct claims
        List<BloodRequest> requests = new ArrayList<>();
        List<String> donorTokens = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            BloodRequest req = createTestRequest(primaryReceiver.getId(), "Claim Hosp " + i);
            requests.add(req);
            User d = createUser("ClaimDonor" + i, "claim.donor." + i + "@netra.org", Set.of(UserRole.ROLE_DONOR));
            donorTokens.add(tokenFor(d));

            if (donorProfileRepository.findByUserId(d.getId()).isEmpty()) {
                DonorProfile dp = new DonorProfile(d.getId(), BloodGroup.O_POSITIVE, DonorAvailabilityStatus.AVAILABLE);
                dp.setBloodGroupVerificationStatus(BloodGroupVerificationStatus.VERIFIED);
                donorProfileRepository.save(dp);
            }

            DonorMatch dm = new DonorMatch(req.getId(), d.getId(), Instant.now(), Instant.now().plus(24, ChronoUnit.HOURS));
            dm.accept(Instant.now());
            donorMatchRepository.save(dm);
        }

        // Warm-up (10)
        for (int i = 0; i < 10; i++) {
            CreateDonationClaimRequest claimReq = new CreateDonationClaimRequest(
                    DonationSourceType.BLOOD_REQUEST, requests.get(i).getId(), null, LocalDate.now(), "Warmup notes");
            mockMvc.perform(post("/api/v1/donations")
                    .header("Authorization", donorTokens.get(i))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(claimReq)))
                    .andExpect(status().isCreated());
        }

        int iterations = 50;
        List<Long> latencies = new ArrayList<>(iterations);
        int errors = 0;
        long startTotal = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            int idx = i + 10;
            CreateDonationClaimRequest claimReq = new CreateDonationClaimRequest(
                    DonationSourceType.BLOOD_REQUEST, requests.get(idx).getId(), null, LocalDate.now(), "Measured notes");
            String payload = objectMapper.writeValueAsString(claimReq);

            long t0 = System.nanoTime();
            try {
                MvcResult res = mockMvc.perform(post("/api/v1/donations")
                        .header("Authorization", donorTokens.get(idx))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                        .andReturn();
                long t1 = System.nanoTime();
                latencies.add(t1 - t0);
                if (res.getResponse().getStatus() != 201) errors++;
            } catch (Exception e) {
                errors++;
            }
        }
        long durationTotal = System.nanoTime() - startTotal;
        BenchmarkResult result = new BenchmarkResult("8. Donation Claim", latencies, errors, durationTotal / 1_000_000_000.0);
        BENCHMARK_RESULTS.add(result);
        log.info("BENCHMARK RESULT: {}", result);
        assertTrue(result.getErrorRate() == 0.0);
    }

    // =========================================================================
    // 9. DONATION VERIFICATION (50 measured, 10 warm-up)
    // =========================================================================

    @Test
    @Order(9)
    @DisplayName("Benchmark 9: Donation Verification HTTP Latency & Throughput")
    void benchmark09_DonationVerification() throws Exception {
        // Pre-create 60 pending donations
        List<Donation> pendingDonations = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            BloodRequest req = createTestRequest(primaryReceiver.getId(), "Ver Hosp " + i);
            User d = createUser("VerDonor" + i, "ver.donor." + i + "@netra.org", Set.of(UserRole.ROLE_DONOR));
            Donation don = new Donation(d.getId(), DonationSourceType.BLOOD_REQUEST, req.getId(), null, LocalDate.now(), "Notes");
            pendingDonations.add(donationRepository.save(don));
        }

        VerifyDonationRequest verifyReq = new VerifyDonationRequest("Verified clinically");
        String payload = objectMapper.writeValueAsString(verifyReq);

        // Warm-up (10)
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/donations/" + pendingDonations.get(i).getId() + "/verify")
                    .header("Authorization", staffToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                    .andExpect(status().isOk());
        }

        int iterations = 50;
        List<Long> latencies = new ArrayList<>(iterations);
        int errors = 0;
        long startTotal = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            int idx = i + 10;
            long t0 = System.nanoTime();
            try {
                MvcResult res = mockMvc.perform(post("/api/v1/donations/" + pendingDonations.get(idx).getId() + "/verify")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                        .andReturn();
                long t1 = System.nanoTime();
                latencies.add(t1 - t0);
                if (res.getResponse().getStatus() != 200) errors++;
            } catch (Exception e) {
                errors++;
            }
        }
        long durationTotal = System.nanoTime() - startTotal;
        BenchmarkResult result = new BenchmarkResult("9. Donation Verification", latencies, errors, durationTotal / 1_000_000_000.0);
        BENCHMARK_RESULTS.add(result);
        log.info("BENCHMARK RESULT: {}", result);
        assertTrue(result.getErrorRate() == 0.0);
    }

    // =========================================================================
    // 10. FULFILLMENT CREATION (50 measured, 10 warm-up)
    // =========================================================================

    @Test
    @Order(10)
    @DisplayName("Benchmark 10: Fulfillment Creation HTTP Latency & Throughput")
    void benchmark10_FulfillmentCreation() throws Exception {
        // Pre-create 60 verified donations & requests
        List<BloodRequest> reqs = new ArrayList<>();
        List<Donation> dons = new ArrayList<>();

        for (int i = 0; i < 60; i++) {
            BloodRequest req = createTestRequest(primaryReceiver.getId(), "Fulfill Hosp " + i);
            reqs.add(req);

            User d = createUser("FulfillDonor" + i, "ff.donor." + i + "@netra.org", Set.of(UserRole.ROLE_DONOR));
            Donation don = new Donation(d.getId(), DonationSourceType.BLOOD_REQUEST, req.getId(), null, LocalDate.now(), "Notes");
            don.verify(bankStaff.getId(), Instant.now(), "Clinically verified");
            dons.add(donationRepository.save(don));
        }

        // Warm-up (10)
        for (int i = 0; i < 10; i++) {
            CreateFulfillmentRequest fReq = new CreateFulfillmentRequest(reqs.get(i).getId(), dons.get(i).getId(), 1, "Warmup fulfillment");
            mockMvc.perform(post("/api/v1/fulfillments")
                    .header("Authorization", staffToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(fReq)))
                    .andExpect(status().isCreated());
        }

        int iterations = 50;
        List<Long> latencies = new ArrayList<>(iterations);
        int errors = 0;
        long startTotal = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            int idx = i + 10;
            CreateFulfillmentRequest fReq = new CreateFulfillmentRequest(reqs.get(idx).getId(), dons.get(idx).getId(), 1, "Measured fulfillment");
            String payload = objectMapper.writeValueAsString(fReq);

            long t0 = System.nanoTime();
            try {
                MvcResult res = mockMvc.perform(post("/api/v1/fulfillments")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                        .andReturn();
                long t1 = System.nanoTime();
                latencies.add(t1 - t0);
                if (res.getResponse().getStatus() != 201) errors++;
            } catch (Exception e) {
                errors++;
            }
        }
        long durationTotal = System.nanoTime() - startTotal;
        BenchmarkResult result = new BenchmarkResult("10. Fulfillment Creation", latencies, errors, durationTotal / 1_000_000_000.0);
        BENCHMARK_RESULTS.add(result);
        log.info("BENCHMARK RESULT: {}", result);
        assertTrue(result.getErrorRate() == 0.0);
    }

    // =========================================================================
    // 11. FULFILLMENT COMPLETION (50 measured, 10 warm-up)
    // =========================================================================

    @Test
    @Order(11)
    @DisplayName("Benchmark 11: Fulfillment Completion HTTP Latency & Throughput")
    void benchmark11_FulfillmentCompletion() throws Exception {
        // Pre-create 60 fulfillments and transition them to IN_PROGRESS
        List<Fulfillment> fulfillments = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            BloodRequest req = createTestRequest(primaryReceiver.getId(), "Complete Hosp " + i);
            User d = createUser("CompDonor" + i, "comp.donor." + i + "@netra.org", Set.of(UserRole.ROLE_DONOR));
            Donation don = new Donation(d.getId(), DonationSourceType.BLOOD_REQUEST, req.getId(), null, LocalDate.now(), "Notes");
            don.verify(bankStaff.getId(), Instant.now(), "Clinically verified");
            don = donationRepository.save(don);

            Fulfillment f = new Fulfillment(req.getId(), don.getId(), 1, bankStaff.getId(), "Completing");
            f.start(bankStaff.getId(), Instant.now());
            fulfillments.add(fulfillmentRepository.save(f));
        }

        // Warm-up (10)
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/fulfillments/" + fulfillments.get(i).getId() + "/complete")
                    .header("Authorization", staffToken))
                    .andExpect(status().isOk());
        }

        int iterations = 50;
        List<Long> latencies = new ArrayList<>(iterations);
        int errors = 0;
        long startTotal = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            int idx = i + 10;
            long t0 = System.nanoTime();
            try {
                MvcResult res = mockMvc.perform(post("/api/v1/fulfillments/" + fulfillments.get(idx).getId() + "/complete")
                        .header("Authorization", staffToken))
                        .andReturn();
                long t1 = System.nanoTime();
                latencies.add(t1 - t0);
                if (res.getResponse().getStatus() != 200) errors++;
            } catch (Exception e) {
                errors++;
            }
        }
        long durationTotal = System.nanoTime() - startTotal;
        BenchmarkResult result = new BenchmarkResult("11. Fulfillment Completion", latencies, errors, durationTotal / 1_000_000_000.0);
        BENCHMARK_RESULTS.add(result);
        log.info("BENCHMARK RESULT: {}", result);
        assertTrue(result.getErrorRate() == 0.0);
    }

    @AfterAll
    static void printBenchmarkSummary() {
        System.out.println("\n=========================================================================================================");
        System.out.println("                              NETRA PERFORMANCE BASELINE BENCHMARK SUMMARY (HTTP API)");
        System.out.println("=========================================================================================================");
        System.out.printf("%-32s | %6s | %9s | %9s | %9s | %9s | %9s | %10s | %6s%n",
                "Operation", "Count", "Avg (ms)", "p50 (ms)", "p95 (ms)", "p99 (ms)", "Min (ms)", "Throughput", "Errors");
        System.out.println("---------------------------------------------------------------------------------------------------------");
        for (BenchmarkResult r : BENCHMARK_RESULTS) {
            System.out.printf("%-32s | %6d | %9.2f | %9.2f | %9.2f | %9.2f | %9.2f | %8.1f/s | %5.1f%%%n",
                    r.getOperationName(), r.getTotalRequests(), r.getAvgLatencyMs(), r.getP50Ms(), r.getP95Ms(),
                    r.getP99Ms(), r.getMinMs(), r.getThroughput(), r.getErrorRate());
        }
        System.out.println("=========================================================================================================\n");
    }

    private BloodRequest createTestRequest(UUID requesterId, String hospital) {
        BloodRequest r = new BloodRequest();
        r.setRequesterUserId(requesterId);
        r.setBloodGroup(BloodGroup.O_POSITIVE);
        r.setUnitsRequired(1);
        r.setUnitsFulfilled(0);
        r.setStatus(BloodRequestStatus.OPEN);
        r.setUrgency(BloodRequestUrgency.NORMAL);
        r.setHospitalName(hospital);
        r.setHospitalAddress("100 Main St");
        r.setCity("Mumbai");
        r.setState("Maharashtra");
        r.setPostalCode("400001");
        r.setLatitude(19.0760);
        r.setLongitude(72.8777);
        r.setRequiredBy(Instant.now().plus(48, ChronoUnit.HOURS));
        return bloodRequestRepository.save(r);
    }
}
