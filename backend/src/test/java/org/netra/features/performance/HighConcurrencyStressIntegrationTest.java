package org.netra.features.performance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.netra.core.exception.ValidationException;
import org.netra.core.security.SecurityUtils;
import org.netra.features.bloodbank.entity.BloodBank;
import org.netra.features.bloodbank.entity.BloodBankAccount;
import org.netra.features.bloodbank.entity.BloodBankAccountStatus;
import org.netra.features.bloodbank.entity.BloodBankOperatingStatus;
import org.netra.features.bloodbank.entity.BloodBankVerificationStatus;
import org.netra.features.bloodbank.entity.BloodInventory;
import org.netra.features.bloodbank.repository.BloodBankAccountRepository;
import org.netra.features.bloodbank.repository.BloodBankRepository;
import org.netra.features.bloodbank.repository.BloodInventoryRepository;
import org.netra.features.bloodbank.service.BloodInventoryService;
import org.netra.features.bloodrequest.dto.CancelBloodRequestRequest;
import org.netra.features.bloodrequest.entity.BloodRequest;
import org.netra.features.bloodrequest.entity.BloodRequestStatus;
import org.netra.features.bloodrequest.entity.BloodRequestUrgency;
import org.netra.features.bloodrequest.repository.BloodRequestRepository;
import org.netra.features.bloodrequest.service.BloodRequestService;
import org.netra.features.donation.entity.Donation;
import org.netra.features.donation.entity.DonationSourceType;
import org.netra.features.donation.repository.DonationRepository;
import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.bloodbank.dto.UpdateInventoryRequest;
import org.netra.features.fulfillment.dto.CreateFulfillmentRequest;
import org.netra.features.fulfillment.dto.FulfillmentDto;
import org.netra.features.fulfillment.entity.Fulfillment;
import org.netra.features.fulfillment.entity.FulfillmentStatus;
import org.netra.features.fulfillment.repository.FulfillmentRepository;
import org.netra.features.fulfillment.service.FulfillmentService;
import org.netra.features.matching.entity.DonorMatch;
import org.netra.features.matching.entity.MatchStatus;
import org.netra.features.matching.repository.DonorMatchRepository;
import org.netra.features.matching.service.DonorResponseService;
import org.netra.features.notification.entity.Notification;
import org.netra.features.notification.entity.NotificationReferenceType;
import org.netra.features.notification.entity.NotificationType;
import org.netra.features.notification.repository.NotificationRepository;
import org.netra.features.notification.service.NotificationService;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.entity.UserStatus;
import org.netra.features.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 4 & Phase 5 — High-Concurrency & Lock Contention Stress Test.
 *
 * Stresses concurrency-sensitive operations under real multi-threaded transactional execution:
 * 1. Donor Response Concurrency:
 *    - 1 BloodRequest with multiple distinct DonorMatch records responding concurrently.
 *    - Concurrent double-acceptance on the same match (idempotent / single winner).
 * 2. Fulfillment Allocation Concurrency:
 *    - Multiple operators allocating against a shared BloodRequest.
 *    - Invariant: unitsFulfilled <= unitsRequired, no duplicate allocations, correct final status.
 * 3. Lock Ordering Race:
 *    - Cancellation racing with Fulfillment Completion (lock order: BloodRequest -> Donation -> Fulfillment).
 *    - Invariant: Zero deadlocks, exactly one valid terminal state, mutually exclusive outcome.
 * 4. Blood Inventory Concurrency:
 *    - Concurrent stock adjustments verifying no lost updates and no negative stock.
 * 5. Notification Concurrency:
 *    - Concurrent creation with identical idempotency keys (idempotency, zero duplicate persistence).
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HighConcurrencyStressIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(HighConcurrencyStressIntegrationTest.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private BloodRequestService bloodRequestService;

    @Autowired
    private DonorMatchRepository donorMatchRepository;

    @Autowired
    private DonorResponseService donorResponseService;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private FulfillmentRepository fulfillmentRepository;

    @Autowired
    private FulfillmentService fulfillmentService;

    @Autowired
    private BloodBankRepository bloodBankRepository;

    @Autowired
    private BloodBankAccountRepository bloodBankAccountRepository;

    @Autowired
    private BloodInventoryRepository bloodInventoryRepository;

    @Autowired
    private BloodInventoryService bloodInventoryService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private User requester;
    private User staffUser;
    private BloodBank bloodBank;

    @BeforeEach
    void setUp() {
        if (requester == null) {
            requester = createUser("Req Concurrency", "req.conc@netra.org", Set.of(UserRole.ROLE_RECEIVER));
            staffUser = createUser("Staff Concurrency", "staff.conc@netra.org", Set.of(UserRole.ROLE_BLOODBANK));

            bloodBank = bloodBankRepository.findAll().stream().findFirst().orElseGet(() -> {
                BloodBank bb = new BloodBank("Concurrency Blood Center", "BB-CONC-01", "77 Station Rd",
                        "Mumbai", "Maharashtra", "400001", 19.0760, 72.8777, "+912288889999",
                        "conc.bank@netra.org", BloodBankVerificationStatus.VERIFIED, BloodBankOperatingStatus.OPEN);
                return bloodBankRepository.save(bb);
            });

            if (!bloodBankAccountRepository.existsByUserIdAndBloodBankIdAndStatus(staffUser.getId(), bloodBank.getId(), BloodBankAccountStatus.ACTIVE)) {
                BloodBankAccount link = new BloodBankAccount(staffUser.getId(), bloodBank.getId(), BloodBankAccountStatus.ACTIVE);
                bloodBankAccountRepository.save(link);
            }
        }
    }

    private User createUser(String name, String email, Set<UserRole> roles) {
        return userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            User u = new User(name, email.toLowerCase(), "+919876543210", passwordEncoder.encode("SecretPass123"), roles);
            u.setStatus(UserStatus.ACTIVE);
            return userRepository.save(u);
        });
    }

    private void authenticate(User user) {
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority(r.name()))
                .toList();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user.getId(), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // =========================================================================
    // 1. DONOR RESPONSE CONCURRENCY
    // =========================================================================

    @Test
    @DisplayName("Concurrency 1A: Multiple distinct donors responding concurrently to the same BloodRequest")
    void testConcurrentDonorResponses_DistinctMatches_SameBloodRequest() throws Exception {
        BloodRequest req = createBloodRequest(requester.getId(), 2, "Response Camp Hospital");

        int donorCount = 8;
        List<User> donors = new ArrayList<>();
        List<DonorMatch> matches = new ArrayList<>();

        for (int i = 0; i < donorCount; i++) {
            User d = createUser("ConcDonor " + i, "concdonor." + i + "@netra.org", Set.of(UserRole.ROLE_DONOR));
            donors.add(d);
            DonorMatch dm = new DonorMatch(req.getId(), d.getId(), Instant.now(), Instant.now().plus(24, ChronoUnit.HOURS));
            matches.add(donorMatchRepository.save(dm));
        }

        ExecutorService executor = Executors.newFixedThreadPool(donorCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(donorCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < donorCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    authenticate(donors.get(idx));
                    donorResponseService.acceptMatch(matches.get(idx).getId(), donors.get(idx).getId(), "127.0.0.1", "ConcAgent");
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    log.error("Donor response failed for donor {}: {}", idx, e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "All donor response threads must finish within timeout");
        executor.shutdown();

        // Invariant: All distinct donor matches should accept cleanly without state corruption
        assertEquals(donorCount, successCount.get(), "All distinct donor responses should succeed");

        List<DonorMatch> finalMatches = donorMatchRepository.findAllById(matches.stream().map(DonorMatch::getId).toList());
        for (DonorMatch m : finalMatches) {
            assertEquals(MatchStatus.ACCEPTED, m.getResponseStatus());
            assertNotNull(m.getRespondedAt());
        }

        // BloodRequest must remain open and uncorrupted
        BloodRequest reloadedReq = bloodRequestRepository.findById(req.getId()).orElseThrow();
        assertEquals(BloodRequestStatus.OPEN, reloadedReq.getStatus());
    }

    @Test
    @DisplayName("Concurrency 1B: Concurrent double-acceptance on the SAME DonorMatch")
    void testConcurrentAcceptance_SameDonorMatch_OnlyOneSucceeds() throws Exception {
        BloodRequest req = createBloodRequest(requester.getId(), 1, "Double Accept Hospital");
        User donor = createUser("Solo Donor", "solo.donor@netra.org", Set.of(UserRole.ROLE_DONOR));
        DonorMatch match = donorMatchRepository.save(new DonorMatch(req.getId(), donor.getId(), Instant.now(), Instant.now().plus(24, ChronoUnit.HOURS)));

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    authenticate(donor);
                    donorResponseService.acceptMatch(match.getId(), donor.getId(), "127.0.0.1", "ConcAgent");
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Invariant: Exactly one acceptance must succeed; all subsequent concurrent calls must fail
        assertEquals(1, successCount.get(), "Exactly one acceptance must succeed on the same match");
        assertEquals(threads - 1, failureCount.get(), "Subsequent acceptance attempts must be rejected");

        DonorMatch reloaded = donorMatchRepository.findById(match.getId()).orElseThrow();
        assertEquals(MatchStatus.ACCEPTED, reloaded.getResponseStatus());
    }

    // =========================================================================
    // 2. FULFILLMENT CONCURRENCY & QUANTITY ACCOUNTING
    // =========================================================================

    @Test
    @DisplayName("Concurrency 2: Multiple operators allocating fulfillment against shared BloodRequest")
    void testConcurrentFulfillmentAllocation_UnitsFulfilledNeverExceedsUnitsRequired() throws Exception {
        // Request requires 2 units
        int unitsRequired = 2;
        BloodRequest req = createBloodRequest(requester.getId(), unitsRequired, "Allocation Care Hospital");

        // 6 distinct verified donations
        int candidates = 6;
        List<Donation> donations = new ArrayList<>();
        for (int i = 0; i < candidates; i++) {
            User d = createUser("FfDonor " + i, "ffdonor." + i + "@netra.org", Set.of(UserRole.ROLE_DONOR));
            Donation don = new Donation(d.getId(), DonationSourceType.BLOOD_REQUEST, req.getId(), null, LocalDate.now(), "Notes");
            don.verify(staffUser.getId(), Instant.now(), "Verified");
            donations.add(donationRepository.save(don));
        }

        // 6 concurrent operator threads attempt to ALLOCATE (create) fulfillment on the same request
        ExecutorService executor = Executors.newFixedThreadPool(candidates);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(candidates);

        AtomicInteger allocatedCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);
        List<FulfillmentDto> allocatedFulfillments = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < candidates; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    authenticate(staffUser);
                    FulfillmentDto dto = fulfillmentService.createFulfillment(
                            new CreateFulfillmentRequest(req.getId(), donations.get(idx).getId(), 1, "Ready F " + idx),
                            "127.0.0.1", "ConcAgent");
                    allocatedFulfillments.add(dto);
                    allocatedCount.incrementAndGet();
                } catch (Exception e) {
                    rejectedCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Invariant Verification:
        // 1. unitsFulfilled / allocated <= unitsRequired (exactly 2 can be allocated)
        assertEquals(unitsRequired, allocatedCount.get(), "Exactly " + unitsRequired + " fulfillments can be allocated");
        assertEquals(candidates - unitsRequired, rejectedCount.get(), "Excess fulfillment allocations must be rejected");

        // Now start and complete the successfully allocated fulfillments
        authenticate(staffUser);
        for (FulfillmentDto dto : allocatedFulfillments) {
            fulfillmentService.startFulfillment(dto.getId(), "127.0.0.1", "ConcAgent");
            fulfillmentService.completeFulfillment(dto.getId(), "127.0.0.1", "ConcAgent");
        }

        // 2. Final BloodRequest status must be FULFILLED and unitsFulfilled == unitsRequired
        BloodRequest finalReq = bloodRequestRepository.findById(req.getId()).orElseThrow();
        assertEquals(BloodRequestStatus.FULFILLED, finalReq.getStatus(), "BloodRequest must transition to FULFILLED");
        assertEquals(unitsRequired, finalReq.getUnitsFulfilled(), "unitsFulfilled must exactly match unitsRequired");

        // 3. Exactly 2 fulfillments in FULFILLED status
        long completedFulfillments = fulfillmentRepository.findAllById(allocatedFulfillments.stream().map(FulfillmentDto::getId).toList())
                .stream().filter(f -> f.getStatus() == FulfillmentStatus.FULFILLED).count();
        assertEquals(unitsRequired, completedFulfillments, "Exactly 2 fulfillments in database should be FULFILLED");
    }

    // =========================================================================
    // 3. LOCK CONTENTION: CANCELLATION RACING WITH FULFILLMENT
    // =========================================================================

    @Test
    @DisplayName("Concurrency 3: BloodRequest Cancellation racing with Fulfillment Completion")
    void testCancellationRacingWithFulfillment_EnforcesStrictLockOrderAndMutualExclusion() throws Exception {
        BloodRequest req = createBloodRequest(requester.getId(), 1, "Race Condition Hospital");

        User donor = createUser("Race Donor", "race.donor@netra.org", Set.of(UserRole.ROLE_DONOR));
        Donation donation = new Donation(donor.getId(), DonationSourceType.BLOOD_REQUEST, req.getId(), null, LocalDate.now(), "Notes");
        donation.verify(staffUser.getId(), Instant.now(), "Verified");
        donation = donationRepository.save(donation);

        authenticate(staffUser);
        FulfillmentDto fDto = fulfillmentService.createFulfillment(
                new CreateFulfillmentRequest(req.getId(), donation.getId(), 1, "Race F"), "127.0.0.1", "ConcAgent");
        fulfillmentService.startFulfillment(fDto.getId(), "127.0.0.1", "ConcAgent");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger cancelSuccess = new AtomicInteger(0);
        AtomicInteger fulfillSuccess = new AtomicInteger(0);

        // Thread 1: Requester attempts to cancel request
        executor.submit(() -> {
            try {
                startLatch.await();
                authenticate(requester);
                bloodRequestService.cancelRequest(req.getId(), new CancelBloodRequestRequest("Cancelled by requester"), "127.0.0.1", "ConcAgent");
                cancelSuccess.incrementAndGet();
            } catch (Exception e) {
                log.info("Cancellation lost race: {}", e.getMessage());
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread 2: Staff attempts to complete fulfillment
        executor.submit(() -> {
            try {
                startLatch.await();
                authenticate(staffUser);
                fulfillmentService.completeFulfillment(fDto.getId(), "127.0.0.1", "ConcAgent");
                fulfillSuccess.incrementAndGet();
            } catch (Exception e) {
                log.info("Fulfillment lost race: {}", e.getMessage());
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Invariant Verification:
        // Exactly one operation must succeed, mutually exclusive.
        assertEquals(1, cancelSuccess.get() + fulfillSuccess.get(), "Exactly one operation must win the race");

        BloodRequest finalReq = bloodRequestRepository.findById(req.getId()).orElseThrow();
        Fulfillment finalFulfillment = fulfillmentRepository.findById(fDto.getId()).orElseThrow();

        if (fulfillSuccess.get() == 1) {
            assertEquals(BloodRequestStatus.FULFILLED, finalReq.getStatus());
            assertEquals(FulfillmentStatus.FULFILLED, finalFulfillment.getStatus());
        } else {
            assertEquals(BloodRequestStatus.CANCELLED, finalReq.getStatus());
            assertNotEquals(FulfillmentStatus.FULFILLED, finalFulfillment.getStatus());
        }
    }

    // =========================================================================
    // 4. INVENTORY CONCURRENCY
    // =========================================================================

    @Test
    @DisplayName("Concurrency 4: Concurrent blood inventory updates verify no lost updates")
    void testConcurrentInventoryUpdates_ZeroLostUpdates() throws Exception {
        BloodInventory inv = bloodInventoryRepository.findByBloodBankIdAndBloodGroup(bloodBank.getId(), BloodGroup.B_POSITIVE)
                .orElseGet(() -> {
                    BloodInventory bi = new BloodInventory(bloodBank.getId(), BloodGroup.B_POSITIVE, 100);
                    return bloodInventoryRepository.save(bi);
                });
        inv.setUnitsAvailable(100);
        inv = bloodInventoryRepository.saveAndFlush(inv);
        final UUID inventoryId = inv.getId();

        int threadCount = 10;
        int operationsPerThread = 10;
        int totalExpectedOps = threadCount * operationsPerThread;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successfulOps = new AtomicInteger(0);

        // 10 threads each perform 10 successful +1 operations with retry-on-conflict (expected +100 units, final = 200)
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    TransactionTemplate tt = new TransactionTemplate(transactionManager);
                    for (int j = 0; j < operationsPerThread; j++) {
                        boolean updated = false;
                        while (!updated && !Thread.currentThread().isInterrupted()) {
                            try {
                                tt.execute(status -> {
                                    BloodInventory fresh = bloodInventoryRepository.findById(inventoryId).orElseThrow();
                                    fresh.setUnitsAvailable(fresh.getUnitsAvailable() + 1);
                                    fresh.setLastUpdatedAt(Instant.now());
                                    fresh.setUpdatedAt(Instant.now());
                                    bloodInventoryRepository.saveAndFlush(fresh);
                                    return true;
                                });
                                updated = true;
                                successfulOps.incrementAndGet();
                            } catch (Exception e) {
                                // Optimistic lock conflict: retry with randomized backoff jitter
                                try {
                                    Thread.sleep(2 + ThreadLocalRandom.current().nextInt(20));
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Inventory update thread failed", e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        BloodInventory finalInv = bloodInventoryRepository.findById(inventoryId).orElseThrow();
        assertEquals(totalExpectedOps, successfulOps.get(), "Exact number of successful operations must be 100");
        assertEquals(200, finalInv.getUnitsAvailable(), "Final inventory units must be EXACTLY 200 (100 initial + 100 successful operations)");
    }

    // =========================================================================
    // 5. NOTIFICATION CONCURRENCY & IDEMPOTENCY
    // =========================================================================

    @Test
    @DisplayName("Concurrency 5: Concurrent notification dispatch with identical idempotency key")
    void testConcurrentNotificationDispatch_IdempotentDeduplication() throws Exception {
        String sharedIdempotencyKey = "shared-notif-key-" + UUID.randomUUID();
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    notificationService.createNotification(
                            requester.getId(),
                            NotificationType.MATCH_CREATED,
                            "Urgent Match Found",
                            "A donor is nearby",
                            NotificationReferenceType.DONOR_MATCH,
                            UUID.randomUUID(),
                            sharedIdempotencyKey
                    );
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    log.info("Idempotent notification catch: {}", e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Invariant: Zero duplicate notifications in database
        List<Notification> records = notificationRepository.findAll().stream()
                .filter(n -> sharedIdempotencyKey.equals(n.getIdempotencyKey()))
                .toList();
        assertEquals(1, records.size(), "Exactly 1 notification must be persisted for a unique idempotency key");
        assertTrue(successCount.get() >= 1, "At least one call must return successfully");
    }

    private BloodRequest createBloodRequest(UUID requesterId, int units, String hospital) {
        BloodRequest r = new BloodRequest();
        r.setRequesterUserId(requesterId);
        r.setBloodGroup(BloodGroup.O_POSITIVE);
        r.setUnitsRequired(units);
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
