package org.netra.features.bloodbank;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.core.audit.SecurityAuditLog;
import org.netra.core.audit.SecurityAuditLogRepository;
import org.netra.features.bloodbank.dto.UpdateInventoryRequest;
import org.netra.features.bloodbank.entity.BloodBank;
import org.netra.features.bloodbank.entity.BloodBankOperatingStatus;
import org.netra.features.bloodbank.entity.BloodBankVerificationStatus;
import org.netra.features.bloodbank.entity.BloodInventory;
import org.netra.features.bloodbank.repository.BloodBankRepository;
import org.netra.features.bloodbank.repository.BloodInventoryRepository;
import org.netra.features.bloodbank.service.BloodInventoryService;
import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.entity.UserStatus;
import org.netra.features.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BloodInventoryConcurrencyTest {

    @Autowired
    private BloodBankRepository bloodBankRepository;

    @Autowired
    private BloodInventoryRepository bloodInventoryRepository;

    @Autowired
    private BloodInventoryService bloodInventoryService;

    @Autowired
    private SecurityAuditLogRepository securityAuditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private User createAdminUser() {
        String email = "concurrency.admin." + UUID.randomUUID() + "@netra.org";
        User user = new User(
                "Concurrency Admin",
                email,
                "+919876543210",
                passwordEncoder.encode("SecurePass123"),
                Set.of(UserRole.ROLE_ADMIN)
        );
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private BloodBank createTestBank() {
        BloodBank bank = new BloodBank();
        bank.setName("Concurrency Test Blood Centre");
        bank.setAddress("100 Concurrency Boulevard");
        bank.setCity("Mumbai");
        bank.setState("Maharashtra");
        bank.setPostalCode("400001");
        bank.setLatitude(18.9401);
        bank.setLongitude(72.8347);
        bank.setPhone("+912224170000");
        bank.setVerificationStatus(BloodBankVerificationStatus.VERIFIED);
        bank.setOperatingStatus(BloodBankOperatingStatus.OPEN);
        return bloodBankRepository.save(bank);
    }

    @Test
    @DisplayName("Concurrency 1: Concurrent same-row updates produce exactly 1 success, 1 conflict, no lost updates")
    void testConcurrentSameRowInventoryUpdateConflict() throws Exception {
        User admin = createAdminUser();
        BloodBank bank = createTestBank();

        // 1. Initial stock: O+ = 1 unit, version = 0
        BloodInventory initialStock = new BloodInventory(bank.getId(), BloodGroup.O_POSITIVE, 1);
        BloodInventory savedStock = bloodInventoryRepository.saveAndFlush(initialStock);
        UUID stockId = savedStock.getId();
        assertEquals(0L, savedStock.getVersion());

        // Baseline audit count
        long initialAuditCount = securityAuditLogRepository.findAll().stream()
                .filter(l -> "INVENTORY_UPDATED".equals(l.getEventType()))
                .count();

        // 2. Setup concurrency synchronization
        CountDownLatch readBothLatch = new CountDownLatch(2);
        CountDownLatch commitStartLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Thread 1: attempts setting units to 0
        executor.submit(() -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new UsernamePasswordAuthenticationToken(
                    admin.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
            SecurityContextHolder.setContext(context);

            TransactionTemplate tt = new TransactionTemplate(transactionManager);
            try {
                tt.execute(status -> {
                    // Read entity with version 0
                    BloodInventory entity = bloodInventoryRepository.findById(stockId).orElseThrow();
                    assertEquals(0L, entity.getVersion());

                    readBothLatch.countDown();
                    try {
                        commitStartLatch.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    entity.setUnitsAvailable(0);
                    entity.setLastUpdatedAt(Instant.now());
                    bloodInventoryRepository.saveAndFlush(entity);

                    // Audit only on success
                    securityAuditLogRepository.save(new SecurityAuditLog(
                            "INVENTORY_UPDATED", admin.getId(), "127.0.0.1", "TestClient", "{\"units\":0}"));
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

        // Thread 2: attempts setting units to 4 using the same starting version 0
        executor.submit(() -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new UsernamePasswordAuthenticationToken(
                    admin.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
            SecurityContextHolder.setContext(context);

            TransactionTemplate tt = new TransactionTemplate(transactionManager);
            try {
                tt.execute(status -> {
                    // Read entity with version 0
                    BloodInventory entity = bloodInventoryRepository.findById(stockId).orElseThrow();
                    assertEquals(0L, entity.getVersion());

                    readBothLatch.countDown();
                    try {
                        commitStartLatch.await(5, TimeUnit.SECONDS);
                        // Brief pause to ensure Thread 1 commits first
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    entity.setUnitsAvailable(4);
                    entity.setLastUpdatedAt(Instant.now());
                    bloodInventoryRepository.saveAndFlush(entity);

                    // Audit only on success
                    securityAuditLogRepository.save(new SecurityAuditLog(
                            "INVENTORY_UPDATED", admin.getId(), "127.0.0.1", "TestClient", "{\"units\":4}"));
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

        // Wait until both read version 0, then release latch
        assertTrue(readBothLatch.await(5, TimeUnit.SECONDS), "Both threads must read starting version 0");
        commitStartLatch.countDown();
        assertTrue(finishLatch.await(10, TimeUnit.SECONDS), "Both threads must finish execution");
        executor.shutdown();

        // 3. Assertions: exactly 1 success, exactly 1 conflict
        assertEquals(1, successCount.get(), "Exactly one transaction must succeed");
        assertEquals(1, conflictCount.get(), "Exactly one transaction must fail with an optimistic locking conflict");

        // 4. Assert database consistency
        BloodInventory finalStock = bloodInventoryRepository.findById(stockId).orElseThrow();
        assertEquals(1L, finalStock.getVersion(), "Version must increment exactly once to 1");
        assertEquals(0, finalStock.getUnitsAvailable(), "Stock must reflect the winning transaction");
        assertTrue(finalStock.getUnitsAvailable() >= 0, "Inventory must never be negative");

        // 5. Assert audit log consistency: exactly one audit record written
        long finalAuditCount = securityAuditLogRepository.findAll().stream()
                .filter(l -> "INVENTORY_UPDATED".equals(l.getEventType()))
                .count();
        assertEquals(initialAuditCount + 1, finalAuditCount, "Exactly one INVENTORY_UPDATED audit record must exist");
    }

    @Test
    @DisplayName("Concurrency 2: Concurrent row creation produces exactly 1 success and 1 unique constraint conflict")
    void testConcurrentInventoryRowCreationConflict() throws Exception {
        User admin = createAdminUser();
        BloodBank bank = createTestBank();

        // Ensure no initial A- row exists
        assertFalse(bloodInventoryRepository.existsByBloodBankIdAndBloodGroup(bank.getId(), BloodGroup.A_NEGATIVE));

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        for (int i = 0; i < 2; i++) {
            final int units = (i + 1) * 5;
            executor.submit(() -> {
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(new UsernamePasswordAuthenticationToken(
                        admin.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
                SecurityContextHolder.setContext(context);

                TransactionTemplate tt = new TransactionTemplate(transactionManager);
                try {
                    startLatch.await(5, TimeUnit.SECONDS);
                    tt.execute(status -> {
                        BloodInventory newRow = new BloodInventory(bank.getId(), BloodGroup.A_NEGATIVE, units);
                        return bloodInventoryRepository.saveAndFlush(newRow);
                    });
                    successCount.incrementAndGet();
                } catch (DataIntegrityViolationException e) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    if (e.getCause() instanceof DataIntegrityViolationException
                            || e.getMessage().contains("uq_blood_inventory_bank_group")
                            || e.getMessage().contains("Unique index")) {
                        conflictCount.incrementAndGet();
                    }
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(finishLatch.await(10, TimeUnit.SECONDS), "Both creation threads must complete");
        executor.shutdown();

        // Assertions: exactly 1 success, 1 conflict
        assertEquals(1, successCount.get(), "Exactly one creation must succeed");
        assertEquals(1, conflictCount.get(), "Competing creation must fail with unique constraint conflict");

        // Database must contain exactly 1 row for this bank + A-
        List<BloodInventory> rows = bloodInventoryRepository.findByBloodBankId(bank.getId()).stream()
                .filter(inv -> inv.getBloodGroup() == BloodGroup.A_NEGATIVE)
                .toList();
        assertEquals(1, rows.size(), "Database must contain exactly ONE inventory record for (bankId, A-)");
        assertTrue(rows.get(0).getUnitsAvailable() >= 0);
    }
}
