package org.netra.features.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.core.security.JwtTokenProvider;
import org.netra.features.bloodbank.entity.BloodBank;
import org.netra.features.bloodbank.entity.BloodBankVerificationStatus;
import org.netra.features.bloodbank.repository.BloodBankRepository;
import org.netra.features.events.entity.DonationEvent;
import org.netra.features.events.entity.DonationEventRegistration;
import org.netra.features.events.entity.DonationEventRegistrationStatus;
import org.netra.features.events.entity.DonationEventStatus;
import org.netra.features.events.entity.DonationEventType;
import org.netra.features.events.repository.DonationEventRegistrationRepository;
import org.netra.features.events.repository.DonationEventRepository;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.entity.UserStatus;
import org.netra.features.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
class DonationEventRegistrationConcurrencyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DonationEventRepository donationEventRepository;

    @Autowired
    private DonationEventRegistrationRepository registrationRepository;

    @Autowired
    private BloodBankRepository bloodBankRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User createTestUser(String prefix) {
        String email = prefix.toLowerCase() + "." + UUID.randomUUID() + "@netra.org";
        User user = new User(
                prefix + " User",
                email,
                "+919876543210",
                passwordEncoder.encode("SecurePass123"),
                Set.of(UserRole.ROLE_DONOR)
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

    private BloodBank createPersistedBloodBank() {
        BloodBank bank = new BloodBank();
        bank.setName("Concurrency Test Blood Bank " + UUID.randomUUID().toString().substring(0, 8));
        bank.setAddress("456 Health Boulevard");
        bank.setCity("Mumbai");
        bank.setState("Maharashtra");
        bank.setPostalCode("400001");
        bank.setLatitude(18.9401);
        bank.setLongitude(72.8347);
        bank.setPhone("+912223456789");
        bank.setVerificationStatus(BloodBankVerificationStatus.VERIFIED);
        return bloodBankRepository.save(bank);
    }

    private DonationEvent createPublishedEvent(BloodBank bank, User creator, int capacity, int initialCount) {
        Instant now = Instant.now();
        DonationEvent event = new DonationEvent();
        event.setBloodBankId(bank.getId());
        event.setTitle("Blood Donation Camp " + UUID.randomUUID().toString().substring(0, 8));
        event.setVenueName("Community Hall");
        event.setAddress("100 Marine Drive");
        event.setCity("Mumbai");
        event.setState("Maharashtra");
        event.setPostalCode("400020");
        event.setLatitude(18.9401);
        event.setLongitude(72.8347);
        event.setStartAt(now.plus(2, ChronoUnit.DAYS));
        event.setEndAt(now.plus(2, ChronoUnit.DAYS).plus(6, ChronoUnit.HOURS));
        event.setRegistrationOpenAt(now.minus(1, ChronoUnit.DAYS));
        event.setRegistrationCloseAt(now.plus(1, ChronoUnit.DAYS));
        event.setDonorCapacity(capacity);
        event.setCurrentRegistrationCount(initialCount);
        event.setStatus(DonationEventStatus.PUBLISHED);
        event.setEventType(DonationEventType.BLOOD_DONATION_CAMP);
        event.setCreatedBy(creator.getId());
        event.setPublishedAt(now.minus(1, ChronoUnit.DAYS));
        event.setCreatedAt(now);
        event.setUpdatedAt(now);
        return donationEventRepository.save(event);
    }

    @Test
    @DisplayName("Concurrency A: Capacity = 1, two donors register simultaneously -> exactly 1 success (201), 1 EVENT_FULL conflict (409)")
    void testConcurrentRegistrationCapacityOne() throws Exception {
        BloodBank bank = createPersistedBloodBank();
        User creator = createTestUser("CreatorA");
        DonationEvent event = createPublishedEvent(bank, creator, 1, 0);

        User donor1 = createTestUser("DonorA1");
        User donor2 = createTestUser("DonorA2");

        String token1 = getAccessToken(donor1);
        String token2 = getAccessToken(donor2);

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<Integer> statusCodes = Collections.synchronizedList(new ArrayList<>());
        List<String> errorCodes = Collections.synchronizedList(new ArrayList<>());

        Runnable task1 = () -> {
            try {
                startLatch.await();
                MvcResult result = mockMvc.perform(post("/api/v1/donation-events/" + event.getId() + "/register")
                        .header("Authorization", "Bearer " + token1)).andReturn();
                int status = result.getResponse().getStatus();
                statusCodes.add(status);
                if (status == 201) {
                    successCount.incrementAndGet();
                } else if (status == 409) {
                    conflictCount.incrementAndGet();
                    errorCodes.add(objectMapper.readTree(result.getResponse().getContentAsString()).path("error").asText());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                doneLatch.countDown();
            }
        };

        Runnable task2 = () -> {
            try {
                startLatch.await();
                MvcResult result = mockMvc.perform(post("/api/v1/donation-events/" + event.getId() + "/register")
                        .header("Authorization", "Bearer " + token2)).andReturn();
                int status = result.getResponse().getStatus();
                statusCodes.add(status);
                if (status == 201) {
                    successCount.incrementAndGet();
                } else if (status == 409) {
                    conflictCount.incrementAndGet();
                    errorCodes.add(objectMapper.readTree(result.getResponse().getContentAsString()).path("error").asText());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                doneLatch.countDown();
            }
        };

        executor.submit(task1);
        executor.submit(task2);

        startLatch.countDown(); // Fire both threads
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "Concurrent tasks should finish within 10s");
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly 1 registration must succeed");
        assertEquals(1, conflictCount.get(), "Exactly 1 registration must receive 409 Conflict");
        assertTrue(errorCodes.contains("EVENT_FULL"), "Conflict response must have error code EVENT_FULL");

        DonationEvent updatedEvent = donationEventRepository.findById(event.getId()).orElseThrow();
        assertEquals(1, updatedEvent.getCurrentRegistrationCount(), "Current registration count must be exactly 1");

        long slotConsumingRows = registrationRepository.countSlotConsumingRegistrations(
                event.getId(), Set.of(DonationEventRegistrationStatus.REGISTERED, DonationEventRegistrationStatus.CHECKED_IN));
        assertEquals(1, slotConsumingRows, "Actual slot-consuming registrations in DB must be exactly 1");
    }

    @Test
    @DisplayName("Concurrency B: Capacity = 100, count = 99, two donors register simultaneously -> exactly 1 success (201), 1 conflict (409), final count = 100")
    void testConcurrentRegistrationLastSlot() throws Exception {
        BloodBank bank = createPersistedBloodBank();
        User creator = createTestUser("CreatorB");
        DonationEvent event = createPublishedEvent(bank, creator, 100, 99);

        // Pre-insert 99 dummy registrations to maintain relational invariant
        List<DonationEventRegistration> existingRegs = new ArrayList<>();
        for (int i = 0; i < 99; i++) {
            User dummy = createTestUser("DummyB" + i);
            existingRegs.add(new DonationEventRegistration(event.getId(), dummy.getId(), DonationEventRegistrationStatus.REGISTERED));
        }
        registrationRepository.saveAllAndFlush(existingRegs);

        User donor1 = createTestUser("DonorB1");
        User donor2 = createTestUser("DonorB2");

        String token1 = getAccessToken(donor1);
        String token2 = getAccessToken(donor2);

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        executor.submit(() -> {
            try {
                startLatch.await();
                MvcResult result = mockMvc.perform(post("/api/v1/donation-events/" + event.getId() + "/register")
                        .header("Authorization", "Bearer " + token1)).andReturn();
                if (result.getResponse().getStatus() == 201) successCount.incrementAndGet();
                else if (result.getResponse().getStatus() == 409) conflictCount.incrementAndGet();
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                MvcResult result = mockMvc.perform(post("/api/v1/donation-events/" + event.getId() + "/register")
                        .header("Authorization", "Bearer " + token2)).andReturn();
                if (result.getResponse().getStatus() == 201) successCount.incrementAndGet();
                else if (result.getResponse().getStatus() == 409) conflictCount.incrementAndGet();
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly 1 thread should successfully take the 100th slot");
        assertEquals(1, conflictCount.get(), "Exactly 1 thread should be rejected with EVENT_FULL");

        DonationEvent updated = donationEventRepository.findById(event.getId()).orElseThrow();
        assertEquals(100, updated.getCurrentRegistrationCount(), "Registration count must be exactly 100 and never exceed capacity");

        long slotConsumingRows = registrationRepository.countSlotConsumingRegistrations(
                event.getId(), Set.of(DonationEventRegistrationStatus.REGISTERED, DonationEventRegistrationStatus.CHECKED_IN));
        assertEquals(100, slotConsumingRows, "Total slot-consuming rows in DB must equal 100");
    }

    @Test
    @DisplayName("Concurrency C: Same donor double-taps registration concurrently -> exactly 1 success (201), 1 ALREADY_REGISTERED conflict (409), exactly 1 DB row")
    void testConcurrentSameDonorRegistration() throws Exception {
        BloodBank bank = createPersistedBloodBank();
        User creator = createTestUser("CreatorC");
        DonationEvent event = createPublishedEvent(bank, creator, 50, 0);

        User donor = createTestUser("DoubleTapDonor");
        String token = getAccessToken(donor);

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<String> errorCodes = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    MvcResult result = mockMvc.perform(post("/api/v1/donation-events/" + event.getId() + "/register")
                            .header("Authorization", "Bearer " + token)).andReturn();
                    int status = result.getResponse().getStatus();
                    if (status == 201) {
                        successCount.incrementAndGet();
                    } else if (status == 409) {
                        conflictCount.incrementAndGet();
                        errorCodes.add(objectMapper.readTree(result.getResponse().getContentAsString()).path("error").asText());
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(1, successCount.get(), "First registration must succeed (201)");
        assertEquals(1, conflictCount.get(), "Duplicate retry must be rejected with 409 Conflict");
        assertTrue(errorCodes.contains("ALREADY_REGISTERED"), "Error code must be ALREADY_REGISTERED");

        DonationEvent updated = donationEventRepository.findById(event.getId()).orElseThrow();
        assertEquals(1, updated.getCurrentRegistrationCount(), "Current registration count must remain 1");

        long rowCount = registrationRepository.findAll().stream()
                .filter(r -> r.getEventId().equals(event.getId()) && r.getDonorUserId().equals(donor.getId()))
                .count();
        assertEquals(1, rowCount, "Database must contain exactly 1 registration row for this donor and event");
    }

    @Test
    @DisplayName("Concurrency D: Concurrent cancel + register -> count equals actual slot-consuming count, count <= capacity, no negative count")
    void testConcurrentCancelAndRegister() throws Exception {
        BloodBank bank = createPersistedBloodBank();
        User creator = createTestUser("CreatorD");
        DonationEvent event = createPublishedEvent(bank, creator, 1, 1);

        User donor1 = createTestUser("DonorD1");
        DonationEventRegistration reg1 = new DonationEventRegistration(
                event.getId(), donor1.getId(), DonationEventRegistrationStatus.REGISTERED);
        registrationRepository.saveAndFlush(reg1);

        User donor2 = createTestUser("DonorD2");

        String token1 = getAccessToken(donor1);
        String token2 = getAccessToken(donor2);

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        // Thread 1: Donor 1 cancels
        executor.submit(() -> {
            try {
                startLatch.await();
                mockMvc.perform(delete("/api/v1/donation-events/" + event.getId() + "/registration")
                        .header("Authorization", "Bearer " + token1));
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread 2: Donor 2 registers
        executor.submit(() -> {
            try {
                startLatch.await();
                mockMvc.perform(post("/api/v1/donation-events/" + event.getId() + "/register")
                        .header("Authorization", "Bearer " + token2));
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        DonationEvent finalEvent = donationEventRepository.findById(event.getId()).orElseThrow();
        long actualSlotConsumingCount = registrationRepository.countSlotConsumingRegistrations(
                event.getId(), Set.of(DonationEventRegistrationStatus.REGISTERED, DonationEventRegistrationStatus.CHECKED_IN));

        // Strict architectural invariants:
        assertTrue(finalEvent.getCurrentRegistrationCount() >= 0, "Registration count cannot be negative");
        assertTrue(finalEvent.getCurrentRegistrationCount() <= finalEvent.getDonorCapacity(), "Registration count cannot exceed capacity");
        assertEquals(actualSlotConsumingCount, (long) finalEvent.getCurrentRegistrationCount(),
                "CRITICAL INVARIANT: current_registration_count must equal actual slot-consuming registration count in DB");
    }
}
