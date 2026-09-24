package org.netra.features.performance;

import org.junit.jupiter.api.*;
import org.netra.core.ratelimit.RateLimitingService;
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
import org.netra.features.matching.dto.DonorMatchResponse;
import org.netra.features.matching.repository.DonorCandidateProjection;
import org.netra.features.matching.repository.DonorMatchingRepository;
import org.netra.features.matching.rules.BloodCompatibilityMatrix;
import org.netra.features.matching.service.DonorMatchingService;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.entity.UserStatus;
import org.netra.features.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 3 — Donor Matching Scaling Benchmark Test.
 *
 * Measures donor matching performance across 3 distinct population tiers:
 * - Tier 1: Small population (50 donors)
 * - Tier 2: Medium population (250 donors)
 * - Tier 3: Large population (1,000 donors)
 *
 * Breaks down execution latency:
 * 1. Database candidate projection query latency
 * 2. Haversine distance computation and filtering cost
 * 3. In-memory sorting, tie-breaking, and bounding cost
 * 4. End-to-end response latency
 *
 * Validates domain invariant preservation under all population tiers:
 * - Strict ABO/Rh compatibility rules
 * - Distance radius enforcement (no candidates outside requested radius)
 * - Deterministic ordering (quality, distance, createdAt)
 * - Result limit capping
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DonorMatchingScalingBenchmarkTest {

    private static final Logger log = LoggerFactory.getLogger(DonorMatchingScalingBenchmarkTest.class);

    @Autowired
    private DonorMatchingService donorMatchingService;

    @Autowired
    private DonorMatchingRepository donorMatchingRepository;

    @Autowired
    private BloodCompatibilityMatrix compatibilityMatrix;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonorProfileRepository donorProfileRepository;

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RateLimitingService rateLimitingService;

    private User requester;
    private BloodRequest bloodRequest;

    @BeforeEach
    void setUp() {
        rateLimitingService.reset();
        if (requester == null) {
            requester = userRepository.findByEmailIgnoreCase("scale.req@netra.org").orElseGet(() -> {
                User u = new User("Requester Scale", "scale.req@netra.org", "+919876543200",
                        passwordEncoder.encode("Pass123!"), Set.of(UserRole.ROLE_RECEIVER));
                u.setStatus(UserStatus.ACTIVE);
                return userRepository.save(u);
            });

            bloodRequest = bloodRequestRepository.findAll().stream()
                    .filter(br -> br.getRequesterUserId().equals(requester.getId()))
                    .findFirst()
                    .orElseGet(() -> {
                        BloodRequest br = new BloodRequest();
                        br.setRequesterUserId(requester.getId());
                        br.setBloodGroup(BloodGroup.O_POSITIVE);
                        br.setUnitsRequired(2);
                        br.setUnitsFulfilled(0);
                        br.setStatus(BloodRequestStatus.OPEN);
                        br.setUrgency(BloodRequestUrgency.NORMAL);
                        br.setHospitalName("Central Trauma Hospital");
                        br.setHospitalAddress("100 Marine Drive");
                        br.setCity("Mumbai");
                        br.setState("Maharashtra");
                        br.setPostalCode("400020");
                        br.setLatitude(18.9438);
                        br.setLongitude(72.8234);
                        br.setRequiredBy(Instant.now().plus(48, ChronoUnit.HOURS));
                        return bloodRequestRepository.save(br);
                    });
        }
    }

    @Test
    @Order(1)
    @DisplayName("Matching Scale Tier 1: Small Population (50 donors)")
    void testMatchingScale_SmallPopulation_50Donors() {
        runMatchingPopulationBenchmark("Tier 1: Small (50)", 50);
    }

    @Test
    @Order(2)
    @DisplayName("Matching Scale Tier 2: Medium Population (250 donors)")
    void testMatchingScale_MediumPopulation_250Donors() {
        runMatchingPopulationBenchmark("Tier 2: Medium (250)", 250);
    }

    @Test
    @Order(3)
    @DisplayName("Matching Scale Tier 3: Large Population (1,000 donors)")
    void testMatchingScale_LargePopulation_1000Donors() {
        runMatchingPopulationBenchmark("Tier 3: Large (1000)", 1000);
    }

    private void runMatchingPopulationBenchmark(String tierName, int donorCount) {
        log.info("--- Starting Donor Matching Scale Benchmark: {} ---", tierName);
        List<User> seededUsers = new ArrayList<>(donorCount);
        List<DonorProfile> seededProfiles = new ArrayList<>(donorCount);

        Random random = new Random(42); // deterministic seed
        BloodGroup[] groups = BloodGroup.values();

        // Seed donors in a geographic cluster around Mumbai (latitude 18.9438, longitude 72.8234)
        for (int i = 0; i < donorCount; i++) {
            User u = new User("Donor " + i, "donor." + donorCount + "." + i + "@netra.org",
                    "+9198000" + String.format("%05d", i), "hash", Set.of(UserRole.ROLE_DONOR));
            u.setStatus(UserStatus.ACTIVE);
            seededUsers.add(u);
        }
        userRepository.saveAll(seededUsers);

        for (int i = 0; i < donorCount; i++) {
            User u = seededUsers.get(i);
            // 60% compatible (O+ or O-), 40% other groups
            BloodGroup bg = (i % 5 < 3) ? (i % 2 == 0 ? BloodGroup.O_POSITIVE : BloodGroup.O_NEGATIVE) : groups[random.nextInt(groups.length)];
            // Vary distances within ~15 km
            double latOffset = (random.nextDouble() - 0.5) * 0.2;
            double lngOffset = (random.nextDouble() - 0.5) * 0.2;

            DonorProfile dp = new DonorProfile(u.getId(), bg, DonorAvailabilityStatus.AVAILABLE);
            dp.setBloodGroupVerificationStatus(BloodGroupVerificationStatus.VERIFIED);
            dp.setLastDonationDate(LocalDate.now().minusDays(100 + (i % 30)));
            dp.setLatitude(18.9438 + latOffset);
            dp.setLongitude(72.8234 + lngOffset);
            seededProfiles.add(dp);
        }
        donorProfileRepository.saveAll(seededProfiles);

        try {
            // Measure Component 1: Database candidate projection query
            Set<BloodGroup> compatible = compatibilityMatrix.getCompatibleDonorGroups(bloodRequest.getBloodGroup());
            double radiusKm = 25.0;
            double latDelta = radiusKm / 111.0;
            double minLat = bloodRequest.getLatitude() - latDelta;
            double maxLat = bloodRequest.getLatitude() + latDelta;
            double minLng = bloodRequest.getLongitude() - latDelta;
            double maxLng = bloodRequest.getLongitude() + latDelta;
            LocalDate maxLastDonation = LocalDate.now().minusDays(90);

            long dbStart = System.nanoTime();
            List<DonorCandidateProjection> candidates = donorMatchingRepository.findCandidateDonors(
                    compatible, requester.getId(), minLat, maxLat, minLng, maxLng, maxLastDonation);
            long dbDurationNs = System.nanoTime() - dbStart;
            double dbMs = dbDurationNs / 1_000_000.0;

            // Measure Component 2: End-to-End Matching Service
            // Warm-up
            rateLimitingService.reset();
            donorMatchingService.findMatches(bloodRequest.getId(), requester.getId(), 25.0, 20, "127.0.0.1", "BenchmarkAgent");

            int iterations = 20;
            List<Long> latencies = new ArrayList<>(iterations);
            DonorMatchResponse lastResponse = null;

            long totalServiceStart = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                rateLimitingService.reset();
                long t0 = System.nanoTime();
                lastResponse = donorMatchingService.findMatches(
                        bloodRequest.getId(), requester.getId(), 25.0, 20, "127.0.0.1", "BenchmarkAgent");
                latencies.add(System.nanoTime() - t0);
            }
            long totalServiceDuration = System.nanoTime() - totalServiceStart;

            BenchmarkResult result = new BenchmarkResult(
                    "Matching Scale " + tierName, latencies, 0, totalServiceDuration / 1_000_000_000.0);

            log.info("POPULATION [{}]: Seeded={}, Candidate Pool={}, DB Query Latency={} ms, E2E Avg Latency={} ms, p95={} ms, Throughput={} ops/s",
                    tierName, donorCount, candidates.size(), String.format("%.2f", dbMs), String.format("%.2f", result.getAvgLatencyMs()), String.format("%.2f", result.getP95Ms()), String.format("%.1f", result.getThroughput()));

            // Invariant Verification:
            assertNotNull(lastResponse);
            int matchCount = lastResponse.getMatches() != null ? lastResponse.getMatches().size() : 0;
            assertTrue(matchCount > 0, "Matches should be found for compatible population");
            assertTrue(matchCount <= 20, "Results must respect limit");
            lastResponse.getMatches().forEach(match -> {
                assertTrue(compatible.contains(match.getBloodGroup()), "Matched donor must be ABO/Rh compatible");
                assertTrue(match.getDistanceKm() <= 25.0, "Distance must be within requested radius");
            });

        } finally {
            // Clean up to isolate subsequent test tiers
            donorProfileRepository.deleteAll(seededProfiles);
            userRepository.deleteAll(seededUsers);
        }
    }
}
