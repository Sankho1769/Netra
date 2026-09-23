package org.netra.features.fulfillment;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.netra.features.bloodbank.entity.*;
import org.netra.features.bloodbank.repository.BloodBankAccountRepository;
import org.netra.features.bloodbank.repository.BloodBankRepository;
import org.netra.features.bloodrequest.entity.BloodRequest;
import org.netra.features.bloodrequest.entity.BloodRequestStatus;
import org.netra.features.bloodrequest.repository.BloodRequestRepository;
import org.netra.features.donation.entity.Donation;
import org.netra.features.donation.entity.DonationSourceType;
import org.netra.features.donation.entity.DonationVerificationStatus;
import org.netra.features.donation.repository.DonationRepository;
import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.donor.entity.DonorProfile;
import org.netra.features.donor.repository.DonorProfileRepository;
import org.netra.features.fulfillment.dto.CreateFulfillmentRequest;
import org.netra.features.fulfillment.dto.FulfillmentDto;
import org.netra.features.fulfillment.entity.*;
import org.netra.features.fulfillment.repository.FulfillmentRepository;
import org.netra.features.fulfillment.service.FulfillmentService;
import org.netra.features.notification.repository.NotificationRepository;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class FulfillmentConcurrencyTest {

    @Autowired private FulfillmentRepository fulfillmentRepository;
    @Autowired private FulfillmentService fulfillmentService;
    @Autowired private DonationRepository donationRepository;
    @Autowired private BloodRequestRepository bloodRequestRepository;
    @Autowired private DonorProfileRepository donorProfileRepository;
    @Autowired private BloodBankRepository bloodBankRepository;
    @Autowired private BloodBankAccountRepository bloodBankAccountRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PlatformTransactionManager transactionManager;

    private User staff;
    private User requester;
    private User donor1;
    private User donor2;
    private BloodBank bloodBank;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAllInBatch();
        fulfillmentRepository.deleteAllInBatch();
        donationRepository.deleteAllInBatch();
        bloodRequestRepository.deleteAllInBatch();
        donorProfileRepository.deleteAllInBatch();
        bloodBankAccountRepository.deleteAllInBatch();
        bloodBankRepository.deleteAllInBatch();

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        staff = userRepository.save(new User(
                "Staff Member",
                "staff.conc." + suffix + "@test.org",
                "+91981" + suffix,
                passwordEncoder.encode("Pass123!"),
                Set.of(UserRole.ROLE_BLOODBANK)
        ));

        requester = userRepository.save(new User(
                "Requester",
                "req.conc." + suffix + "@test.org",
                "+91982" + suffix,
                passwordEncoder.encode("Pass123!"),
                Set.of(UserRole.ROLE_RECEIVER)
        ));

        donor1 = userRepository.save(new User(
                "Donor 1",
                "donor1.conc." + suffix + "@test.org",
                "+91983" + suffix,
                passwordEncoder.encode("Pass123!"),
                Set.of(UserRole.ROLE_DONOR)
        ));

        DonorProfile p1 = new DonorProfile();
        p1.setUserId(donor1.getId());
        p1.setBloodGroup(BloodGroup.O_POSITIVE);
        donorProfileRepository.save(p1);

        donor2 = userRepository.save(new User(
                "Donor 2",
                "donor2.conc." + suffix + "@test.org",
                "+91984" + suffix,
                passwordEncoder.encode("Pass123!"),
                Set.of(UserRole.ROLE_DONOR)
        ));

        DonorProfile p2 = new DonorProfile();
        p2.setUserId(donor2.getId());
        p2.setBloodGroup(BloodGroup.O_POSITIVE);
        donorProfileRepository.save(p2);

        bloodBank = bloodBankRepository.save(new BloodBank(
                "City Blood Bank", "LIC-CC-" + suffix, "Main St", "City", "State", "100001",
                20.0, 80.0, "1234567890", "bb.conc." + suffix + "@test.org",
                BloodBankVerificationStatus.VERIFIED, BloodBankOperatingStatus.OPEN
        ));

        bloodBankAccountRepository.save(new BloodBankAccount(staff.getId(), bloodBank.getId(), BloodBankAccountStatus.ACTIVE));
    }

    @AfterEach
    void tearDown() {
        clearSecurityContext();
    }

    private void setSecurityContext(UUID userId, String... roles) {
        List<SimpleGrantedAuthority> authorities = Arrays.stream(roles)
                .map(r -> new SimpleGrantedAuthority(r.startsWith("ROLE_") ? r : "ROLE_" + r))
                .toList();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                userId.toString(),
                null,
                authorities
        ));
        SecurityContextHolder.setContext(context);
    }

    private void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Concurrency: Two threads attempting to allocate last unit of BloodRequest - only one succeeds")
    void concurrentFulfillmentCreationSerializesOnRemainingUnits() throws Exception {
        // Create blood request for exactly 1 unit
        BloodRequest req = new BloodRequest();
        req.setRequesterUserId(requester.getId());
        req.setBloodGroup(BloodGroup.O_POSITIVE);
        req.setUnitsRequired(1);
        req.setUnitsFulfilled(0);
        req.setStatus(BloodRequestStatus.OPEN);
        req.setHospitalName("City Clinic");
        req.setHospitalAddress("Clinic Rd");
        req.setCity("City");
        req.setState("State");
        req.setPostalCode("100001");
        req.setLatitude(20.0);
        req.setLongitude(80.0);
        req.setRequiredBy(Instant.now().plusSeconds(86400));
        BloodRequest savedReq = bloodRequestRepository.save(req);

        // Create 2 verified donations (donor1 and donor2)
        Donation d1 = new Donation(donor1.getId(), DonationSourceType.BLOOD_REQUEST, savedReq.getId(), null, LocalDate.now(), "Notes");
        d1.setVerificationStatus(DonationVerificationStatus.VERIFIED);
        d1 = donationRepository.save(d1);

        // We can create a second independent request just to associate donor2's verified donation
        BloodRequest req2 = new BloodRequest();
        req2.setRequesterUserId(requester.getId());
        req2.setBloodGroup(BloodGroup.O_POSITIVE);
        req2.setUnitsRequired(1);
        req2.setHospitalName("City Clinic");
        req2.setHospitalAddress("Clinic Rd");
        req2.setCity("City");
        req2.setState("State");
        req2.setPostalCode("100001");
        req2.setLatitude(20.0);
        req2.setLongitude(80.0);
        req2.setRequiredBy(Instant.now().plusSeconds(86400));
        req2 = bloodRequestRepository.save(req2);

        Donation d2 = new Donation(donor2.getId(), DonationSourceType.BLOOD_REQUEST, req2.getId(), null, LocalDate.now(), "Notes");
        d2.setVerificationStatus(DonationVerificationStatus.VERIFIED);
        d2 = donationRepository.save(d2);

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        List<Future<Boolean>> futures = new ArrayList<>();

        final UUID donationId1 = d1.getId();
        final UUID donationId2 = d2.getId();
        final UUID targetRequestId = savedReq.getId();

        // Thread 1: attempts to allocate d1 to savedReq
        futures.add(executor.submit(() -> {
            setSecurityContext(staff.getId(), "ROLE_BLOODBANK");
            barrier.await();
            try {
                FulfillmentDto dto = fulfillmentService.createFulfillment(
                        new CreateFulfillmentRequest(targetRequestId, donationId1, 1, "Thread 1"),
                        "127.0.0.1", "ConcTest"
                );
                return dto != null;
            } catch (Exception ex) {
                return false;
            } finally {
                clearSecurityContext();
            }
        }));

        // Thread 2: attempts to allocate d2 to savedReq
        futures.add(executor.submit(() -> {
            setSecurityContext(staff.getId(), "ROLE_BLOODBANK");
            barrier.await();
            try {
                FulfillmentDto dto = fulfillmentService.createFulfillment(
                        new CreateFulfillmentRequest(targetRequestId, donationId2, 1, "Thread 2"),
                        "127.0.0.1", "ConcTest"
                );
                return dto != null;
            } catch (Exception ex) {
                return false;
            } finally {
                clearSecurityContext();
            }
        }));

        int successCount = 0;
        int failureCount = 0;
        for (Future<Boolean> f : futures) {
            if (f.get(10, TimeUnit.SECONDS)) {
                successCount++;
            } else {
                failureCount++;
            }
        }

        executor.shutdown();

        // Exactly one thread must succeed, and the other must fail because unitsRequired was 1
        assertEquals(1, successCount, "Exactly one thread must succeed in allocating the final unit");
        assertEquals(1, failureCount, "Second thread must be rejected due to zero remaining units");
    }

    @Test
    @DisplayName("Concurrency 2: Concurrent updates based on same version produce exactly 1 success and 1 OptimisticLockingFailureException")
    void testConcurrentUpdatesOptimisticLockConflict() throws Exception {
        // Create blood request for 2 units
        BloodRequest req = new BloodRequest();
        req.setRequesterUserId(requester.getId());
        req.setBloodGroup(BloodGroup.O_POSITIVE);
        req.setUnitsRequired(2);
        req.setUnitsFulfilled(0);
        req.setStatus(BloodRequestStatus.OPEN);
        req.setHospitalName("City Clinic");
        req.setHospitalAddress("Clinic Rd");
        req.setCity("City");
        req.setState("State");
        req.setPostalCode("100001");
        req.setLatitude(20.0);
        req.setLongitude(80.0);
        req.setRequiredBy(Instant.now().plusSeconds(86400));
        BloodRequest savedReq = bloodRequestRepository.save(req);

        // Create verified donation
        Donation d = new Donation(donor1.getId(), DonationSourceType.BLOOD_REQUEST, savedReq.getId(), null, LocalDate.now(), "Notes");
        d.setVerificationStatus(DonationVerificationStatus.VERIFIED);
        d = donationRepository.save(d);

        // Create fulfillment with version 0
        Fulfillment fulfillment = new Fulfillment(savedReq.getId(), d.getId(), 1, staff.getId(), "Initial Note");
        Fulfillment saved = fulfillmentRepository.saveAndFlush(fulfillment);
        UUID fulfillmentId = saved.getId();
        assertEquals(0L, saved.getVersion());

        CountDownLatch readBothLatch = new CountDownLatch(2);
        CountDownLatch commitStartLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Thread 1: Read version 0, start fulfillment, save
        executor.submit(() -> {
            setSecurityContext(staff.getId(), "ROLE_BLOODBANK");
            TransactionTemplate tt = new TransactionTemplate(transactionManager);
            try {
                tt.execute(status -> {
                    Fulfillment entity = fulfillmentRepository.findById(fulfillmentId).orElseThrow();
                    assertEquals(0L, entity.getVersion());

                    readBothLatch.countDown();
                    try {
                        commitStartLatch.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    entity.start(staff.getId(), Instant.now());
                    fulfillmentRepository.saveAndFlush(entity);
                    return true;
                });
                successCount.incrementAndGet();
            } catch (OptimisticLockingFailureException e) {
                conflictCount.incrementAndGet();
            } catch (Exception e) {
                if (e.getCause() instanceof OptimisticLockingFailureException) {
                    conflictCount.incrementAndGet();
                }
            } finally {
                clearSecurityContext();
                finishLatch.countDown();
            }
        });

        // Thread 2: Read version 0, cancel fulfillment, save
        executor.submit(() -> {
            setSecurityContext(staff.getId(), "ROLE_BLOODBANK");
            TransactionTemplate tt = new TransactionTemplate(transactionManager);
            try {
                tt.execute(status -> {
                    Fulfillment entity = fulfillmentRepository.findById(fulfillmentId).orElseThrow();
                    assertEquals(0L, entity.getVersion());

                    readBothLatch.countDown();
                    try {
                        commitStartLatch.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    entity.cancel(staff.getId(), "Concurrent cancel", Instant.now());
                    fulfillmentRepository.saveAndFlush(entity);
                    return true;
                });
                successCount.incrementAndGet();
            } catch (OptimisticLockingFailureException e) {
                conflictCount.incrementAndGet();
            } catch (Exception e) {
                if (e.getCause() instanceof OptimisticLockingFailureException) {
                    conflictCount.incrementAndGet();
                }
            } finally {
                clearSecurityContext();
                finishLatch.countDown();
            }
        });

        assertTrue(readBothLatch.await(5, TimeUnit.SECONDS), "Both threads must read before commit begins");
        commitStartLatch.countDown();
        assertTrue(finishLatch.await(10, TimeUnit.SECONDS), "Both threads must finish execution");
        executor.shutdown();

        // Exactly one commit succeeds and the other encounters optimistic locking conflict
        assertEquals(1, successCount.get(), "Exactly one update must succeed");
        assertEquals(1, conflictCount.get(), "Exactly one update must fail with optimistic lock conflict");

        // Database row state reflects version incremented by 1
        Fulfillment finalState = fulfillmentRepository.findById(fulfillmentId).orElseThrow();
        assertEquals(1L, finalState.getVersion());
        assertTrue(finalState.getStatus() == org.netra.features.fulfillment.entity.FulfillmentStatus.IN_PROGRESS 
                || finalState.getStatus() == org.netra.features.fulfillment.entity.FulfillmentStatus.CANCELLED);
    }
}
