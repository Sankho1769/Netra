package org.netra.features.bloodrequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.core.exception.ValidationException;
import org.netra.core.security.JwtTokenProvider;
import org.netra.features.bloodrequest.dto.CancelBloodRequestRequest;
import org.netra.features.bloodrequest.entity.BloodRequest;
import org.netra.features.bloodrequest.entity.BloodRequestStatus;
import org.netra.features.bloodrequest.entity.BloodRequestUrgency;
import org.netra.features.bloodrequest.repository.BloodRequestRepository;
import org.netra.features.bloodrequest.service.BloodRequestService;
import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.entity.UserStatus;
import org.netra.features.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BloodRequestConcurrencyTest {

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private BloodRequestService bloodRequestService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private User requesterUser;

    @BeforeEach
    void setUp() {
        bloodRequestRepository.deleteAll();

        requesterUser = new User(
                "Concurrency Requester",
                "concurrency.req." + UUID.randomUUID() + "@netra.org",
                "+919876543210",
                passwordEncoder.encode("SecurePass123"),
                Set.of(UserRole.ROLE_RECEIVER)
        );
        requesterUser.setStatus(UserStatus.ACTIVE);
        requesterUser = userRepository.save(requesterUser);
    }

    private BloodRequest createSampleOpenRequest() {
        BloodRequest req = new BloodRequest();
        req.setRequesterUserId(requesterUser.getId());
        req.setBloodGroup(BloodGroup.O_POSITIVE);
        req.setUnitsRequired(2);
        req.setUrgency(BloodRequestUrgency.NORMAL);
        req.setStatus(BloodRequestStatus.OPEN);
        req.setHospitalName("City General Hospital");
        req.setHospitalAddress("100 Central Road");
        req.setCity("Mumbai");
        req.setState("Maharashtra");
        req.setPostalCode("400001");
        req.setLatitude(18.9401);
        req.setLongitude(72.8347);
        req.setRequiredBy(Instant.now().plus(48, ChronoUnit.HOURS));
        req.setCreatedAt(Instant.now());
        req.setUpdatedAt(Instant.now());
        return bloodRequestRepository.saveAndFlush(req);
    }

    @Test
    @DisplayName("Concurrency 1: Concurrent updates based on same version produce exactly 1 success and 1 OptimisticLockingFailureException")
    void testConcurrentUpdatesOptimisticLockConflict() throws Exception {
        BloodRequest initial = createSampleOpenRequest();
        UUID requestId = initial.getId();
        assertEquals(0L, initial.getVersion());

        CountDownLatch readBothLatch = new CountDownLatch(2);
        CountDownLatch commitStartLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Thread 1: Read version 0, update units to 5, save
        executor.submit(() -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new UsernamePasswordAuthenticationToken(
                    requesterUser.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_RECEIVER"))));
            SecurityContextHolder.setContext(context);

            TransactionTemplate tt = new TransactionTemplate(transactionManager);
            try {
                tt.execute(status -> {
                    BloodRequest entity = bloodRequestRepository.findById(requestId).orElseThrow();
                    assertEquals(0L, entity.getVersion());

                    readBothLatch.countDown();
                    try {
                        commitStartLatch.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    entity.setUnitsRequired(5);
                    entity.setUpdatedAt(Instant.now());
                    bloodRequestRepository.saveAndFlush(entity);
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
                finishLatch.countDown();
            }
        });

        // Thread 2: Read version 0, update units to 8, save
        executor.submit(() -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new UsernamePasswordAuthenticationToken(
                    requesterUser.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_RECEIVER"))));
            SecurityContextHolder.setContext(context);

            TransactionTemplate tt = new TransactionTemplate(transactionManager);
            try {
                tt.execute(status -> {
                    BloodRequest entity = bloodRequestRepository.findById(requestId).orElseThrow();
                    assertEquals(0L, entity.getVersion());

                    readBothLatch.countDown();
                    try {
                        commitStartLatch.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    entity.setUnitsRequired(8);
                    entity.setUpdatedAt(Instant.now());
                    bloodRequestRepository.saveAndFlush(entity);
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
        BloodRequest finalState = bloodRequestRepository.findById(requestId).orElseThrow();
        assertEquals(1L, finalState.getVersion());
        assertTrue(finalState.getUnitsRequired() == 5 || finalState.getUnitsRequired() == 8);
    }

    @Test
    @DisplayName("Concurrency 2: Multiple concurrent cancellations result in exactly 1 success; terminal status is immutable")
    void testConcurrentCancellations() throws Exception {
        BloodRequest initial = createSampleOpenRequest();
        UUID requestId = initial.getId();

        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(new UsernamePasswordAuthenticationToken(
                        requesterUser.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_RECEIVER"))));
                SecurityContextHolder.setContext(context);

                try {
                    startLatch.await(5, TimeUnit.SECONDS);
                    CancelBloodRequestRequest cancelReq = new CancelBloodRequestRequest();
                    cancelReq.setReason("Cancel attempt " + index);
                    bloodRequestService.cancelRequest(requestId, cancelReq, "127.0.0.1", "TestClient");
                    successCount.incrementAndGet();
                } catch (ValidationException | OptimisticLockingFailureException e) {
                    rejectedCount.incrementAndGet();
                } catch (Exception e) {
                    if (e.getCause() instanceof OptimisticLockingFailureException) {
                        rejectedCount.incrementAndGet();
                    }
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(finishLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Exactly one cancellation succeeds
        assertEquals(1, successCount.get(), "Exactly one cancellation must succeed");
        assertEquals(threadCount - 1, rejectedCount.get(), "All other cancellations must be rejected");

        // Final state is CANCELLED
        BloodRequest finalState = bloodRequestRepository.findById(requestId).orElseThrow();
        assertEquals(BloodRequestStatus.CANCELLED, finalState.getStatus());
        assertEquals(requesterUser.getId(), finalState.getCancelledBy());
        assertNotNull(finalState.getCancelledAt());
    }
}
