package org.netra.features.matching;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.core.exception.ValidationException;
import org.netra.features.bloodrequest.entity.BloodRequest;
import org.netra.features.bloodrequest.entity.BloodRequestStatus;
import org.netra.features.bloodrequest.entity.BloodRequestUrgency;
import org.netra.features.bloodrequest.repository.BloodRequestRepository;
import org.netra.features.donor.entity.*;
import org.netra.features.donor.repository.DonorProfileRepository;
import org.netra.features.matching.dto.CreateDonorMatchRequest;
import org.netra.features.matching.entity.DonorMatch;
import org.netra.features.matching.entity.MatchStatus;
import org.netra.features.matching.repository.DonorMatchRepository;
import org.netra.features.matching.service.DonorMatchLifecycleService;
import org.netra.features.matching.service.DonorResponseService;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.entity.UserStatus;
import org.netra.features.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DonorResponseConcurrencyTest {

    @Autowired
    private DonorMatchRepository donorMatchRepository;

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private DonorProfileRepository donorProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DonorResponseService donorResponseService;

    @Autowired
    private DonorMatchLifecycleService donorMatchLifecycleService;

    private User requester;
    private User donorUser;
    private DonorProfile donorProfile;
    private BloodRequest bloodRequest;

    @BeforeEach
    void setUp() {
        donorMatchRepository.deleteAll();
        donorProfileRepository.deleteAll();
        bloodRequestRepository.deleteAll();

        requester = createTestUser("req.conc", UserRole.ROLE_RECEIVER, UserStatus.ACTIVE);
        donorUser = createTestUser("donor.conc", UserRole.ROLE_DONOR, UserStatus.ACTIVE);

        DonorProfile dp = new DonorProfile(donorUser.getId(), BloodGroup.A_POSITIVE, DonorAvailabilityStatus.AVAILABLE);
        dp.setBloodGroupVerificationStatus(BloodGroupVerificationStatus.VERIFIED);
        dp.setDonorStatus(DonorStatus.ACTIVE);
        dp.setLatitude(18.9450);
        dp.setLongitude(72.8380);
        donorProfile = donorProfileRepository.save(dp);

        BloodRequest req = new BloodRequest();
        req.setRequesterUserId(requester.getId());
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
        bloodRequest = bloodRequestRepository.save(req);
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

    @Test
    @DisplayName("Concurrency: Concurrent double accept results in exactly one ACCEPTED and one conflict failure")
    void testConcurrentDoubleAccept() throws Exception {
        DonorMatch match = donorMatchRepository.save(
                new DonorMatch(bloodRequest.getId(), donorUser.getId(), Instant.now().plus(24, ChronoUnit.HOURS))
        );

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    donorResponseService.acceptMatch(match.getId(), donorUser.getId(), "127.0.0.1", "TestAgent");
                    successCount.incrementAndGet();
                } catch (Exception ex) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly one accept must succeed");
        assertEquals(1, failureCount.get(), "Concurrent second accept must encounter conflict");

        DonorMatch fresh = donorMatchRepository.findById(match.getId()).orElseThrow();
        assertEquals(MatchStatus.ACCEPTED, fresh.getResponseStatus());
        assertEquals(1L, fresh.getVersion(), "Optimistic lock version incremented exactly once");
    }

    @Test
    @DisplayName("Concurrency: Concurrent accept vs request cancellation results in exactly one valid terminal state")
    void testConcurrentAcceptVsCancellation() throws Exception {
        DonorMatch match = donorMatchRepository.save(
                new DonorMatch(bloodRequest.getId(), donorUser.getId(), Instant.now().plus(24, ChronoUnit.HOURS))
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        // Thread 1: Donor tries to accept
        executor.submit(() -> {
            try {
                startLatch.await();
                donorResponseService.acceptMatch(match.getId(), donorUser.getId(), "127.0.0.1", "TestAgent");
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread 2: Requester cancels active matches
        executor.submit(() -> {
            try {
                startLatch.await();
                donorMatchLifecycleService.cancelActiveMatchesForRequest(bloodRequest.getId(), requester.getId(), "127.0.0.1", "TestAgent");
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        DonorMatch fresh = donorMatchRepository.findById(match.getId()).orElseThrow();
        assertTrue(fresh.getResponseStatus() == MatchStatus.ACCEPTED || fresh.getResponseStatus() == MatchStatus.CANCELLED,
                "Final DB status must be exactly ACCEPTED or CANCELLED, but was: " + fresh.getResponseStatus());
    }

    @Test
    @DisplayName("Concurrency: Concurrent accept vs match expiration results in exactly one valid terminal state")
    void testConcurrentAcceptVsExpiration() throws Exception {
        // Match expires around now
        Instant expiry = Instant.now().plusMillis(20);
        DonorMatch match = donorMatchRepository.save(
                new DonorMatch(bloodRequest.getId(), donorUser.getId(), expiry)
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        // Thread 1: Donor tries to accept
        executor.submit(() -> {
            try {
                startLatch.await();
                donorResponseService.acceptMatch(match.getId(), donorUser.getId(), "127.0.0.1", "TestAgent");
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread 2: Scheduler expires overdue matches
        executor.submit(() -> {
            try {
                startLatch.await();
                donorMatchLifecycleService.expireOverdueMatches(Instant.now().plusSeconds(1));
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        DonorMatch fresh = donorMatchRepository.findById(match.getId()).orElseThrow();
        assertTrue(fresh.getResponseStatus() == MatchStatus.ACCEPTED || fresh.getResponseStatus() == MatchStatus.EXPIRED,
                "Final DB status must be exactly ACCEPTED or EXPIRED, but was: " + fresh.getResponseStatus());
    }

    @Test
    @DisplayName("Concurrency: Concurrent duplicate match creation results in exactly one database record")
    void testConcurrentDuplicateMatchCreation() throws Exception {
        int threads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger createdCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        CreateDonorMatchRequest reqDto = new CreateDonorMatchRequest(donorProfile.getId());

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    donorResponseService.createMatch(bloodRequest.getId(), reqDto, requester.getId(), "127.0.0.1", "TestAgent");
                    createdCount.incrementAndGet();
                } catch (Exception ex) {
                    conflictCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(1, createdCount.get(), "Only one match creation should succeed");
        assertEquals(threads - 1, conflictCount.get(), "All other concurrent creations must fail with conflict");

        List<DonorMatch> records = donorMatchRepository.findByBloodRequestIdOrderByCreatedAtDesc(bloodRequest.getId());
        assertEquals(1, records.size(), "Exactly one DB record must exist");
    }

    @Test
    @DisplayName("Concurrency: Concurrent createMatch operations enforce maxActiveMatchesPerRequest <= 10")
    void testConcurrentActiveMatchLimit_EnforcesMaxActiveMatches() throws Exception {
        int threads = 11;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger createdCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        // Pre-create 11 distinct eligible donor users and profiles
        List<CreateDonorMatchRequest> matchRequests = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            User u = createTestUser("limit.donor" + i, UserRole.ROLE_DONOR, UserStatus.ACTIVE);
            DonorProfile p = new DonorProfile(u.getId(), BloodGroup.A_POSITIVE, DonorAvailabilityStatus.AVAILABLE);
            p.setBloodGroupVerificationStatus(BloodGroupVerificationStatus.VERIFIED);
            p.setDonorStatus(DonorStatus.ACTIVE);
            p.setLatitude(18.9450);
            p.setLongitude(72.8380);
            p = donorProfileRepository.save(p);
            matchRequests.add(new CreateDonorMatchRequest(p.getId()));
        }

        for (int i = 0; i < threads; i++) {
            final CreateDonorMatchRequest reqDto = matchRequests.get(i);
            executor.submit(() -> {
                try {
                    startLatch.await();
                    donorResponseService.createMatch(bloodRequest.getId(), reqDto, requester.getId(), "127.0.0.1", "TestAgent");
                    createdCount.incrementAndGet();
                } catch (Exception ex) {
                    rejectedCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(10, createdCount.get(), "Exactly 10 match creations should succeed (limit = 10)");
        assertEquals(1, rejectedCount.get(), "At least 1 creation must be rejected due to active limit");

        List<DonorMatch> records = donorMatchRepository.findByBloodRequestIdOrderByCreatedAtDesc(bloodRequest.getId());
        assertEquals(10, records.size(), "Active MATCHED count in DB must be exactly 10 (<= maxActiveMatchesPerRequest)");
    }
}
