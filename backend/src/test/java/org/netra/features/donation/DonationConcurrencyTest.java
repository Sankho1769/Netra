package org.netra.features.donation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.features.bloodbank.entity.BloodBank;
import org.netra.features.bloodbank.entity.BloodBankAccount;
import org.netra.features.bloodbank.entity.BloodBankAccountStatus;
import org.netra.features.bloodbank.entity.BloodBankOperatingStatus;
import org.netra.features.bloodbank.entity.BloodBankVerificationStatus;
import org.netra.features.bloodbank.repository.BloodBankAccountRepository;
import org.netra.features.bloodbank.repository.BloodBankRepository;
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
import org.netra.features.donation.service.DonationService;
import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.donor.entity.DonorAvailabilityStatus;
import org.netra.features.donor.entity.DonorProfile;
import org.netra.features.donor.entity.DonorStatus;
import org.netra.features.donor.repository.DonorProfileRepository;
import org.netra.features.matching.entity.DonorMatch;
import org.netra.features.matching.entity.MatchStatus;
import org.netra.features.matching.repository.DonorMatchRepository;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class DonationConcurrencyTest {

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private DonationService donationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonorProfileRepository donorProfileRepository;

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private DonorMatchRepository donorMatchRepository;

    @Autowired
    private BloodBankRepository bloodBankRepository;

    @Autowired
    private BloodBankAccountRepository bloodBankAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User donorUser;
    private User staff1;
    private User staff2;
    private BloodBank bloodBank;
    private BloodRequest bloodRequest;

    @BeforeEach
    void setUp() {
        donationRepository.deleteAll();
        donorMatchRepository.deleteAll();
        bloodRequestRepository.deleteAll();
        donorProfileRepository.deleteAll();
        bloodBankAccountRepository.deleteAll();
        bloodBankRepository.deleteAll();

        donorUser = userRepository.save(new User(
                "Donor Concurrent",
                "donor.conc." + UUID.randomUUID() + "@test.org",
                "+919800111222",
                passwordEncoder.encode("Pass123!"),
                Set.of(UserRole.ROLE_DONOR)
        ));

        DonorProfile profile = new DonorProfile(donorUser.getId(), BloodGroup.O_POSITIVE, DonorAvailabilityStatus.AVAILABLE);
        profile.setDonorStatus(DonorStatus.ACTIVE);
        donorProfileRepository.save(profile);

        staff1 = userRepository.save(new User(
                "Staff 1",
                "staff1.conc." + UUID.randomUUID() + "@test.org",
                "+919800111223",
                passwordEncoder.encode("Pass123!"),
                Set.of(UserRole.ROLE_BLOODBANK)
        ));

        staff2 = userRepository.save(new User(
                "Staff 2",
                "staff2.conc." + UUID.randomUUID() + "@test.org",
                "+919800111224",
                passwordEncoder.encode("Pass123!"),
                Set.of(UserRole.ROLE_BLOODBANK)
        ));

        bloodBank = bloodBankRepository.save(new BloodBank(
                "Central Bank",
                "REG-CONC",
                "Hospital Rd",
                "Kolkata",
                "WB",
                "700001",
                22.57,
                88.36,
                "+913322110000",
                "central@bank.org",
                BloodBankVerificationStatus.VERIFIED,
                BloodBankOperatingStatus.OPEN
        ));

        bloodBankAccountRepository.save(new BloodBankAccount(staff1.getId(), bloodBank.getId(), BloodBankAccountStatus.ACTIVE));
        bloodBankAccountRepository.save(new BloodBankAccount(staff2.getId(), bloodBank.getId(), BloodBankAccountStatus.ACTIVE));

        BloodRequest req = new BloodRequest();
        req.setRequesterUserId(staff1.getId());
        req.setBloodGroup(BloodGroup.O_POSITIVE);
        req.setUnitsRequired(1);
        req.setUrgency(BloodRequestUrgency.NORMAL);
        req.setStatus(BloodRequestStatus.OPEN);
        req.setHospitalName("Metro Hospital");
        req.setHospitalAddress("Park Street");
        req.setCity("Kolkata");
        req.setState("WB");
        req.setPostalCode("700001");
        req.setLatitude(22.57);
        req.setLongitude(88.36);
        req.setRequiredBy(Instant.now().plus(24, ChronoUnit.HOURS));
        bloodRequest = bloodRequestRepository.save(req);

        DonorMatch match = new DonorMatch(
                UUID.randomUUID(),
                bloodRequest.getId(),
                donorUser.getId(),
                MatchStatus.ACCEPTED,
                Instant.now(),
                Instant.now().plus(24, ChronoUnit.HOURS)
        );
        donorMatchRepository.save(match);
    }

    @Test
    @DisplayName("Concurrent duplicate claims for same donor and blood request: exactly one succeeds")
    void testConcurrentDuplicateClaimRace() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    CreateDonationClaimRequest req = new CreateDonationClaimRequest(
                            DonationSourceType.BLOOD_REQUEST,
                            bloodRequest.getId(),
                            null,
                            LocalDate.now(),
                            "Concurrent claim test"
                    );
                    donationService.createClaim(donorUser.getId(), req, "127.0.0.1", "TestAgent");
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        for (Future<?> f : futures) {
            try {
                f.get(10, TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }
        }
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly one claim should succeed in a concurrent race");
        assertEquals(threadCount - 1, failureCount.get(), "All other concurrent duplicate claims must fail");

        List<Donation> donations = donationRepository.findByDonorUserIdOrderByDonationDateDesc(donorUser.getId());
        assertEquals(1, donations.size(), "Only one donation record must exist in repository");
        assertEquals(DonationVerificationStatus.PENDING_VERIFICATION, donations.get(0).getVerificationStatus());
    }

    @Test
    @DisplayName("Concurrent verification race on same donation: only one verification transitions state")
    void testConcurrentVerificationRace() throws InterruptedException {
        // Create initial pending claim
        CreateDonationClaimRequest claimReq = new CreateDonationClaimRequest(
                DonationSourceType.BLOOD_REQUEST,
                bloodRequest.getId(),
                null,
                LocalDate.now(),
                "Claim to verify"
        );
        var created = donationService.createClaim(donorUser.getId(), claimReq, "127.0.0.1", "TestAgent");
        UUID donationId = created.getId();

        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final User staff = (i % 2 == 0) ? staff1 : staff2;
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    VerifyDonationRequest vReq = new VerifyDonationRequest("Concurrent verified");
                    donationService.verifyDonation(donationId, staff.getId(), vReq, "127.0.0.1", "TestAgent");
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        for (Future<?> f : futures) {
            try {
                f.get(10, TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }
        }
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly one concurrent verification call should succeed");
        assertEquals(threadCount - 1, failureCount.get(), "Subsequent concurrent verifications must fail");

        Donation finalDonation = donationRepository.findById(donationId).orElseThrow();
        assertEquals(DonationVerificationStatus.VERIFIED, finalDonation.getVerificationStatus());
        assertNotNull(finalDonation.getVerifiedAt());

        DonorProfile updatedProfile = donorProfileRepository.findByUserId(donorUser.getId()).orElseThrow();
        assertEquals(LocalDate.now(), updatedProfile.getLastDonationDate(), "lastDonationDate must be updated to donation date");
    }
}
