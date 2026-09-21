package org.netra.features.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.features.notification.dto.NotificationDto;
import org.netra.features.notification.dto.RegisterDeviceTokenRequest;
import org.netra.features.notification.entity.*;
import org.netra.features.notification.repository.NotificationRepository;
import org.netra.features.notification.repository.UserDeviceTokenRepository;
import org.netra.features.notification.service.NotificationService;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.entity.UserStatus;
import org.netra.features.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class NotificationConcurrencyTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserDeviceTokenRepository userDeviceTokenRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        userDeviceTokenRepository.deleteAll();

        testUser = new User(
                "Concurrent User",
                "concurrent_" + UUID.randomUUID() + "@test.org",
                "+9198" + String.format("%08d", Math.abs(UUID.randomUUID().hashCode() % 100000000)),
                passwordEncoder.encode("Password123!"),
                Set.of(UserRole.ROLE_DONOR)
        );
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("Concurrency: Concurrent identical events yield strictly 1 persistent notification")
    void testConcurrentDuplicateEvents_Deduplication() throws Exception {
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        String idempotencyKey = "CONCURRENT_MATCH_CREATED:" + UUID.randomUUID();

        List<Future<Notification>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                latch.await();
                return notificationService.createNotification(
                        testUser.getId(),
                        NotificationType.MATCH_CREATED,
                        "Title",
                        "Body",
                        NotificationReferenceType.DONOR_MATCH,
                        UUID.randomUUID(),
                        idempotencyKey
                );
            }));
        }

        latch.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        AtomicInteger successCount = new AtomicInteger(0);
        for (Future<Notification> f : futures) {
            try {
                Notification n = f.get();
                if (n != null) {
                    successCount.incrementAndGet();
                }
            } catch (ExecutionException e) {
                // Should not fail unhandled
                fail("Thread threw unexpected exception: " + e.getCause().getMessage());
            }
        }

        assertEquals(threads, successCount.get());

        // Strictly 1 row in the database
        List<Notification> all = notificationRepository.findAll();
        assertEquals(1, all.size(), "Strictly 1 notification must be persisted despite concurrent creation attempts");
    }

    @Test
    @DisplayName("Concurrency: Simultaneous mark-as-read calls on same notification succeed without locking conflict")
    void testConcurrentMarkAsRead() throws Exception {
        Notification notification = notificationService.createNotification(
                testUser.getId(),
                NotificationType.MATCH_CREATED,
                "Title",
                "Body",
                null,
                null,
                "KEY:mark-read-" + UUID.randomUUID()
        );

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);

        List<Future<NotificationDto>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                latch.await();
                return notificationService.markAsRead(notification.getId(), testUser.getId());
            }));
        }

        latch.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        for (Future<NotificationDto> f : futures) {
            NotificationDto dto = f.get();
            assertTrue(dto.isRead());
            assertNotNull(dto.getReadAt());
        }

        Notification refreshed = notificationRepository.findById(notification.getId()).orElseThrow();
        assertTrue(refreshed.isRead());
    }

    @Test
    @DisplayName("Concurrency: Concurrent device token registration for the same token resolves cleanly")
    void testConcurrentDeviceTokenRegistration() throws Exception {
        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        String sharedToken = "shared_fcm_token_" + UUID.randomUUID();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                latch.await();
                RegisterDeviceTokenRequest req = new RegisterDeviceTokenRequest(sharedToken, DevicePlatform.ANDROID, "FCM");
                return notificationService.registerDeviceToken(testUser.getId(), req);
            }));
        }

        latch.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        for (Future<?> f : futures) {
            assertNotNull(f.get());
        }

        List<UserDeviceToken> tokens = userDeviceTokenRepository.findAll();
        assertEquals(1, tokens.size(), "Strictly 1 device token record for unique token");
        assertTrue(tokens.get(0).isActive());
    }
}
