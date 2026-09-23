package org.netra.features.workflow;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.core.exception.DuplicateResourceException;
import org.netra.core.exception.UnauthorizedSessionAccessException;
import org.netra.core.exception.ValidationException;
import org.netra.features.bloodbank.entity.BloodBank;
import org.netra.features.bloodbank.entity.BloodBankAccount;
import org.netra.features.bloodbank.entity.BloodBankAccountStatus;
import org.netra.features.bloodbank.entity.BloodBankOperatingStatus;
import org.netra.features.bloodbank.entity.BloodBankVerificationStatus;
import org.netra.features.bloodbank.repository.BloodBankAccountRepository;
import org.netra.features.bloodbank.repository.BloodBankRepository;
import org.netra.features.bloodrequest.dto.CancelBloodRequestRequest;
import org.netra.features.bloodrequest.dto.CreateBloodRequestRequest;
import org.netra.features.bloodrequest.dto.BloodRequestDetailDto;
import org.netra.features.bloodrequest.entity.BloodRequest;
import org.netra.features.bloodrequest.entity.BloodRequestStatus;
import org.netra.features.bloodrequest.entity.BloodRequestUrgency;
import org.netra.features.bloodrequest.repository.BloodRequestRepository;
import org.netra.features.bloodrequest.service.BloodRequestService;
import org.netra.features.donation.dto.CreateDonationClaimRequest;
import org.netra.features.donation.dto.DonationDetailDto;
import org.netra.features.donation.dto.RecordVerifiedDonationRequest;
import org.netra.features.donation.dto.VerifyDonationRequest;
import org.netra.features.donation.entity.Donation;
import org.netra.features.donation.entity.DonationSourceType;
import org.netra.features.donation.entity.DonationVerificationStatus;
import org.netra.features.donation.repository.DonationRepository;
import org.netra.features.donation.service.DonationService;
import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.donor.entity.BloodGroupVerificationStatus;
import org.netra.features.donor.entity.DonorAvailabilityStatus;
import org.netra.features.donor.entity.DonorProfile;
import org.netra.features.donor.entity.DonorStatus;
import org.netra.features.donor.repository.DonorProfileRepository;
import org.netra.features.events.entity.DonationEvent;
import org.netra.features.events.entity.DonationEventRegistration;
import org.netra.features.events.entity.DonationEventRegistrationStatus;
import org.netra.features.events.entity.DonationEventStatus;
import org.netra.features.events.entity.DonationEventType;
import org.netra.features.events.repository.DonationEventRegistrationRepository;
import org.netra.features.events.repository.DonationEventRepository;
import org.netra.features.fulfillment.dto.CancelFulfillmentRequest;
import org.netra.features.fulfillment.dto.CreateFulfillmentRequest;
import org.netra.features.fulfillment.dto.FailFulfillmentRequest;
import org.netra.features.fulfillment.dto.FulfillmentDetailDto;
import org.netra.features.fulfillment.dto.FulfillmentDto;
import org.netra.features.fulfillment.entity.Fulfillment;
import org.netra.features.fulfillment.entity.FulfillmentStatus;
import org.netra.features.fulfillment.repository.FulfillmentRepository;
import org.netra.features.fulfillment.service.FulfillmentService;
import org.netra.features.matching.dto.CreateDonorMatchRequest;
import org.netra.features.matching.dto.DonorMatchDetailDto;
import org.netra.features.matching.dto.RequesterDonorMatchDto;
import org.netra.features.matching.entity.DonorMatch;
import org.netra.features.matching.entity.MatchStatus;
import org.netra.features.matching.repository.DonorMatchRepository;
import org.netra.features.matching.service.DonorResponseService;
import org.netra.features.notification.entity.Notification;
import org.netra.features.notification.entity.NotificationType;
import org.netra.features.notification.repository.NotificationRepository;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.entity.UserStatus;
import org.netra.features.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class EndToEndWorkflowIntegrationTest {

    @Autowired private BloodRequestService bloodRequestService;
    @Autowired private BloodRequestRepository bloodRequestRepository;
    @Autowired private DonorResponseService donorResponseService;
    @Autowired private DonorMatchRepository donorMatchRepository;
    @Autowired private DonationService donationService;
    @Autowired private DonationRepository donationRepository;
    @Autowired private FulfillmentService fulfillmentService;
    @Autowired private FulfillmentRepository fulfillmentRepository;
    @Autowired private DonorProfileRepository donorProfileRepository;
    @Autowired private BloodBankRepository bloodBankRepository;
    @Autowired private BloodBankAccountRepository bloodBankAccountRepository;
    @Autowired private DonationEventRepository donationEventRepository;
    @Autowired private DonationEventRegistrationRepository registrationRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PlatformTransactionManager transactionManager;

    private User requester;
    private User donor1;
    private User donor2;
    private User staffBankA;
    private User staffBankB;
    private BloodBank bloodBankA;
    private BloodBank bloodBankB;
    private DonorProfile profile1;
    private DonorProfile profile2;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAllInBatch();
        fulfillmentRepository.deleteAllInBatch();
        donationRepository.deleteAllInBatch();
        registrationRepository.deleteAllInBatch();
        donationEventRepository.deleteAllInBatch();
        donorMatchRepository.deleteAllInBatch();
        bloodRequestRepository.deleteAllInBatch();
        donorProfileRepository.deleteAllInBatch();
        bloodBankAccountRepository.deleteAllInBatch();
        bloodBankRepository.deleteAllInBatch();

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // 1. Create Users
        requester = userRepository.save(new User(
                "E2E Requester", "requester.e2e." + suffix + "@netra.org", "+91981" + suffix,
                passwordEncoder.encode("Pass123!"), Set.of(UserRole.ROLE_RECEIVER)
        ));

        donor1 = userRepository.save(new User(
                "E2E Donor One", "donor1.e2e." + suffix + "@netra.org", "+91982" + suffix,
                passwordEncoder.encode("Pass123!"), Set.of(UserRole.ROLE_DONOR)
        ));

        donor2 = userRepository.save(new User(
                "E2E Donor Two", "donor2.e2e." + suffix + "@netra.org", "+91983" + suffix,
                passwordEncoder.encode("Pass123!"), Set.of(UserRole.ROLE_DONOR)
        ));

        staffBankA = userRepository.save(new User(
                "Staff Bank A", "staffA.e2e." + suffix + "@netra.org", "+91984" + suffix,
                passwordEncoder.encode("Pass123!"), Set.of(UserRole.ROLE_BLOODBANK)
        ));

        staffBankB = userRepository.save(new User(
                "Staff Bank B", "staffB.e2e." + suffix + "@netra.org", "+91985" + suffix,
                passwordEncoder.encode("Pass123!"), Set.of(UserRole.ROLE_BLOODBANK)
        ));

        // 2. Create Donor Profiles (Verified O+ donors, eligible)
        profile1 = new DonorProfile();
        profile1.setUserId(donor1.getId());
        profile1.setBloodGroup(BloodGroup.O_POSITIVE);
        profile1.setBloodGroupVerificationStatus(BloodGroupVerificationStatus.VERIFIED);
        profile1.setDonorStatus(DonorStatus.ACTIVE);
        profile1.setAvailabilityStatus(DonorAvailabilityStatus.AVAILABLE);
        profile1.setLatitude(18.9220);
        profile1.setLongitude(72.8347);
        profile1 = donorProfileRepository.save(profile1);

        profile2 = new DonorProfile();
        profile2.setUserId(donor2.getId());
        profile2.setBloodGroup(BloodGroup.O_POSITIVE);
        profile2.setBloodGroupVerificationStatus(BloodGroupVerificationStatus.VERIFIED);
        profile2.setDonorStatus(DonorStatus.ACTIVE);
        profile2.setAvailabilityStatus(DonorAvailabilityStatus.AVAILABLE);
        profile2.setLatitude(18.9230);
        profile2.setLongitude(72.8350);
        profile2 = donorProfileRepository.save(profile2);

        // 3. Create Blood Banks & Accounts
        bloodBankA = bloodBankRepository.save(new BloodBank(
                "Metro Blood Centre A", "LIC-A-" + suffix, "123 Central Ave", "Mumbai", "Maharashtra", "400001",
                18.9220, 72.8347, "+912222222222", "bankA." + suffix + "@netra.org",
                BloodBankVerificationStatus.VERIFIED, BloodBankOperatingStatus.OPEN
        ));

        bloodBankB = bloodBankRepository.save(new BloodBank(
                "Apex Blood Centre B", "LIC-B-" + suffix, "456 North Rd", "Mumbai", "Maharashtra", "400002",
                18.9250, 72.8380, "+912233333333", "bankB." + suffix + "@netra.org",
                BloodBankVerificationStatus.VERIFIED, BloodBankOperatingStatus.OPEN
        ));

        bloodBankAccountRepository.save(new BloodBankAccount(staffBankA.getId(), bloodBankA.getId(), BloodBankAccountStatus.ACTIVE));
        bloodBankAccountRepository.save(new BloodBankAccount(staffBankB.getId(), bloodBankB.getId(), BloodBankAccountStatus.ACTIVE));
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
    @DisplayName("Complete E2E Domain Flow: Request -> Match -> Accept -> Verify Donation -> Partial Fulfillment -> Final Fulfillment -> Completion")
    void completeEndToEndDomainWorkflow() {
        // Step 1: Requester creates a BloodRequest for 2 units
        setSecurityContext(requester.getId(), "ROLE_RECEIVER");

        CreateBloodRequestRequest createReq = new CreateBloodRequestRequest();
        createReq.setBloodGroup(BloodGroup.O_POSITIVE);
        createReq.setUnitsRequired(2);
        createReq.setUrgency(BloodRequestUrgency.NORMAL);
        createReq.setHospitalName("City Care Hospital");
        createReq.setHospitalAddress("100 Hospital Road");
        createReq.setCity("Mumbai");
        createReq.setState("Maharashtra");
        createReq.setPostalCode("400001");
        createReq.setLatitude(18.9225);
        createReq.setLongitude(72.8348);
        createReq.setRequiredBy(Instant.now().plusSeconds(86400));
        createReq.setDescription("Emergency surgery requirements");

        BloodRequestDetailDto createdRequest = bloodRequestService.createRequest(createReq, "127.0.0.1", "TestClient");
        assertNotNull(createdRequest.getId());
        assertEquals(BloodRequestStatus.OPEN, createdRequest.getStatus());
        assertEquals(2, createdRequest.getUnitsRequired());

        UUID requestId = createdRequest.getId();

        // Step 2: Requester creates DonorMatch for Donor 1
        RequesterDonorMatchDto matchDto1 = donorResponseService.createMatch(
                requestId,
                new CreateDonorMatchRequest(profile1.getId()),
                requester.getId(),
                "127.0.0.1", "TestClient"
        );
        assertNotNull(matchDto1.getMatchId());
        assertEquals(MatchStatus.MATCHED, matchDto1.getResponseStatus());

        // Verify MatchCreated notification for donor1
        List<Notification> donor1Notifs = notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(donor1.getId(), PageRequest.of(0, 10)).getContent();
        assertTrue(donor1Notifs.stream().anyMatch(n -> n.getType() == NotificationType.MATCH_CREATED));

        // Step 3: Donor 1 accepts the match
        setSecurityContext(donor1.getId(), "ROLE_DONOR");
        DonorMatchDetailDto acceptedMatch1 = donorResponseService.acceptMatch(matchDto1.getMatchId(), donor1.getId(), "127.0.0.1", "TestClient");
        assertEquals(MatchStatus.ACCEPTED, acceptedMatch1.getResponseStatus());

        // Verify MatchAccepted notification for requester
        List<Notification> reqNotifs = notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(requester.getId(), PageRequest.of(0, 10)).getContent();
        assertTrue(reqNotifs.stream().anyMatch(n -> n.getType() == NotificationType.MATCH_ACCEPTED));

        // Step 4: Donor 1 donates blood; Staff at Bank A verifies the donation
        setSecurityContext(staffBankA.getId(), "ROLE_BLOODBANK");

        RecordVerifiedDonationRequest recordReq1 = new RecordVerifiedDonationRequest(
                donor1.getId(),
                DonationSourceType.BLOOD_REQUEST,
                requestId,
                null,
                LocalDate.now(),
                "Whole blood 450ml verified at Bank A"
        );
        DonationDetailDto verifiedDonation1 = donationService.recordVerifiedDonation(staffBankA.getId(), recordReq1, "127.0.0.1", "TestClient");
        assertNotNull(verifiedDonation1.getId());
        assertEquals(DonationVerificationStatus.VERIFIED, verifiedDonation1.getVerificationStatus());

        // Verify donor lastDonationDate was updated
        DonorProfile refreshedProfile1 = donorProfileRepository.findByUserId(donor1.getId()).orElseThrow();
        assertEquals(LocalDate.now(), refreshedProfile1.getLastDonationDate());

        // Step 5: Staff creates Fulfillment 1 (1 unit)
        FulfillmentDto fulfillmentDto1 = fulfillmentService.createFulfillment(
                new CreateFulfillmentRequest(requestId, verifiedDonation1.getId(), 1, "First unit allocation"),
                "127.0.0.1", "TestClient"
        );
        assertNotNull(fulfillmentDto1.getId());
        assertEquals(FulfillmentStatus.READY, fulfillmentDto1.getStatus());
        assertEquals(1, fulfillmentDto1.getUnits());

        // Step 6: Staff starts and completes Fulfillment 1 (Partial fulfillment: 1 of 2 units)
        FulfillmentDto startedF1 = fulfillmentService.startFulfillment(fulfillmentDto1.getId(), "127.0.0.1", "TestClient");
        assertEquals(FulfillmentStatus.IN_PROGRESS, startedF1.getStatus());

        FulfillmentDto completedF1 = fulfillmentService.completeFulfillment(startedF1.getId(), "127.0.0.1", "TestClient");
        assertEquals(FulfillmentStatus.FULFILLED, completedF1.getStatus());

        // Verify BloodRequest partial fulfillment: unitsFulfilled = 1, status REMAINS OPEN
        BloodRequest refreshedReqPart = bloodRequestRepository.findById(requestId).orElseThrow();
        assertEquals(1, refreshedReqPart.getUnitsFulfilled());
        assertEquals(BloodRequestStatus.OPEN, refreshedReqPart.getStatus());
        assertNull(refreshedReqPart.getFulfilledAt());

        // Step 7: Second donor (Donor 2) matches, accepts, and donates
        setSecurityContext(requester.getId(), "ROLE_RECEIVER");
        RequesterDonorMatchDto matchDto2 = donorResponseService.createMatch(
                requestId,
                new CreateDonorMatchRequest(profile2.getId()),
                requester.getId(),
                "127.0.0.1", "TestClient"
        );
        assertEquals(MatchStatus.MATCHED, matchDto2.getResponseStatus());

        setSecurityContext(donor2.getId(), "ROLE_DONOR");
        donorResponseService.acceptMatch(matchDto2.getMatchId(), donor2.getId(), "127.0.0.1", "TestClient");

        setSecurityContext(staffBankA.getId(), "ROLE_BLOODBANK");
        RecordVerifiedDonationRequest recordReq2 = new RecordVerifiedDonationRequest(
                donor2.getId(),
                DonationSourceType.BLOOD_REQUEST,
                requestId,
                null,
                LocalDate.now(),
                "Whole blood 450ml verified at Bank A for donor 2"
        );
        DonationDetailDto verifiedDonation2 = donationService.recordVerifiedDonation(staffBankA.getId(), recordReq2, "127.0.0.1", "TestClient");

        // Step 8: Staff creates, starts, and completes Fulfillment 2 (Final unit)
        FulfillmentDto fulfillmentDto2 = fulfillmentService.createFulfillment(
                new CreateFulfillmentRequest(requestId, verifiedDonation2.getId(), 1, "Final unit allocation"),
                "127.0.0.1", "TestClient"
        );
        FulfillmentDto startedF2 = fulfillmentService.startFulfillment(fulfillmentDto2.getId(), "127.0.0.1", "TestClient");
        FulfillmentDto completedF2 = fulfillmentService.completeFulfillment(startedF2.getId(), "127.0.0.1", "TestClient");
        assertEquals(FulfillmentStatus.FULFILLED, completedF2.getStatus());

        // Step 9: Verify BloodRequest is atomically COMPLETED (FULFILLED)
        BloodRequest finalReq = bloodRequestRepository.findById(requestId).orElseThrow();
        assertEquals(2, finalReq.getUnitsFulfilled());
        assertEquals(BloodRequestStatus.FULFILLED, finalReq.getStatus());
        assertNotNull(finalReq.getFulfilledAt());
        assertEquals(staffBankA.getId(), finalReq.getFulfilledBy());

        // Step 10: Verify completion notifications
        List<Notification> finalReqNotifs = notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(requester.getId(), PageRequest.of(0, 10)).getContent();
        assertTrue(finalReqNotifs.stream().anyMatch(n -> n.getType() == NotificationType.FULFILLMENT_COMPLETED && n.getBody().contains("fully completed")));

        List<Notification> donor2Notifs = notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(donor2.getId(), PageRequest.of(0, 10)).getContent();
        assertTrue(donor2Notifs.stream().anyMatch(n -> n.getType() == NotificationType.FULFILLMENT_COMPLETED && n.getTitle().contains("Life Saved!")));

        // Step 11: Invariant - Completed request cannot accept additional fulfillments
        assertThrows(ValidationException.class, () ->
                fulfillmentService.createFulfillment(
                        new CreateFulfillmentRequest(requestId, verifiedDonation2.getId(), 1, "Surplus fulfillment"),
                        "127.0.0.1", "TestClient"
                )
        );
    }

    @Test
    @DisplayName("State Machine Hardening: Per-aggregate invalid transitions are rejected independently")
    void testPerAggregateInvalidTransitions() {
        // Create baseline request & entities
        BloodRequest req = new BloodRequest();
        req.setRequesterUserId(requester.getId());
        req.setBloodGroup(BloodGroup.O_POSITIVE);
        req.setUnitsRequired(1);
        req.setUnitsFulfilled(0);
        req.setStatus(BloodRequestStatus.OPEN);
        req.setHospitalName("City Clinic");
        req.setHospitalAddress("Clinic Rd");
        req.setCity("Mumbai");
        req.setState("Maharashtra");
        req.setPostalCode("400001");
        req.setLatitude(18.9220);
        req.setLongitude(72.8347);
        req.setRequiredBy(Instant.now().plusSeconds(86400));
        BloodRequest savedReq = bloodRequestRepository.save(req);

        // 1. DonorMatch Aggregate State Machine:
        // MATCHED -> ACCEPTED
        DonorMatch match = new DonorMatch(savedReq.getId(), donor1.getId(), Instant.now(), Instant.now().plusSeconds(86400));
        DonorMatch savedMatch = donorMatchRepository.save(match);

        setSecurityContext(donor1.getId(), "ROLE_DONOR");
        donorResponseService.acceptMatch(savedMatch.getId(), donor1.getId(), "127.0.0.1", "TestClient");

        // ACCEPTED -> ACCEPTED must be rejected
        assertThrows(ValidationException.class, () ->
                donorResponseService.acceptMatch(savedMatch.getId(), donor1.getId(), "127.0.0.1", "TestClient"));

        // ACCEPTED -> DECLINED must be rejected
        assertThrows(ValidationException.class, () ->
                donorResponseService.declineMatch(savedMatch.getId(), donor1.getId(), "127.0.0.1", "TestClient"));

        // 2. Donation Aggregate State Machine:
        // PENDING claim cannot be used for fulfillment
        Donation pendingDonation = new Donation(donor1.getId(), DonationSourceType.BLOOD_REQUEST, savedReq.getId(), null, LocalDate.now(), "Pending Claim");
        pendingDonation = donationRepository.save(pendingDonation);

        setSecurityContext(staffBankA.getId(), "ROLE_BLOODBANK");
        final UUID pendingDonId = pendingDonation.getId();
        assertThrows(ValidationException.class, () ->
                fulfillmentService.createFulfillment(
                        new CreateFulfillmentRequest(savedReq.getId(), pendingDonId, 1, "Using pending donation"),
                        "127.0.0.1", "TestClient"
                )
        );

        // REJECTED donation cannot be used for fulfillment
        Donation rejectedDonation = new Donation(donor2.getId(), DonationSourceType.BLOOD_REQUEST, savedReq.getId(), null, LocalDate.now(), "Rejected Claim");
        rejectedDonation.reject(staffBankA.getId(), Instant.now(), "Failed medical check");
        rejectedDonation = donationRepository.save(rejectedDonation);

        final UUID rejectedDonId = rejectedDonation.getId();
        assertThrows(ValidationException.class, () ->
                fulfillmentService.createFulfillment(
                        new CreateFulfillmentRequest(savedReq.getId(), rejectedDonId, 1, "Using rejected donation"),
                        "127.0.0.1", "TestClient"
                )
        );

        // 3. Fulfillment Aggregate State Machine:
        pendingDonation.verify(staffBankA.getId(), Instant.now(), "Verified");
        Donation verifiedDonation = donationRepository.save(pendingDonation);

        FulfillmentDto fDto = fulfillmentService.createFulfillment(
                new CreateFulfillmentRequest(savedReq.getId(), verifiedDonation.getId(), 1, "Test F"),
                "127.0.0.1", "TestClient"
        );

        // READY -> FULFILLED directly (skipping IN_PROGRESS) must be rejected
        assertThrows(ValidationException.class, () ->
                fulfillmentService.completeFulfillment(fDto.getId(), "127.0.0.1", "TestClient"));

        // READY -> FAILED directly (skipping IN_PROGRESS) must be rejected
        assertThrows(ValidationException.class, () ->
                fulfillmentService.failFulfillment(fDto.getId(), new FailFulfillmentRequest("Early fail", null), "127.0.0.1", "TestClient"));

        // Transition READY -> IN_PROGRESS
        fulfillmentService.startFulfillment(fDto.getId(), "127.0.0.1", "TestClient");

        // IN_PROGRESS -> CANCELLED must be rejected (can only fail or complete once started)
        assertThrows(ValidationException.class, () ->
                fulfillmentService.cancelFulfillment(fDto.getId(), new CancelFulfillmentRequest("Cancel in progress", null), "127.0.0.1", "TestClient"));

        // Transition IN_PROGRESS -> FULFILLED
        fulfillmentService.completeFulfillment(fDto.getId(), "127.0.0.1", "TestClient");

        // FULFILLED -> IN_PROGRESS must be rejected
        assertThrows(ValidationException.class, () ->
                fulfillmentService.startFulfillment(fDto.getId(), "127.0.0.1", "TestClient"));

        // FULFILLED -> CANCELLED must be rejected
        assertThrows(ValidationException.class, () ->
                fulfillmentService.cancelFulfillment(fDto.getId(), new CancelFulfillmentRequest("Cancel fulfilled", null), "127.0.0.1", "TestClient"));

        // 4. BloodRequest Aggregate State Machine:
        // Request is now FULFILLED; CANCELLED transition must be rejected
        setSecurityContext(requester.getId(), "ROLE_RECEIVER");
        assertThrows(ValidationException.class, () ->
                bloodRequestService.cancelRequest(savedReq.getId(), new CancelBloodRequestRequest("Cancelled late"), "127.0.0.1", "TestClient"));
    }

    @Test
    @DisplayName("Cross-Tenant Authorization: Staff at Blood Bank B cannot operate event donations of Blood Bank A")
    void testCrossTenantAuthorizationScoping() {
        // Create event hosted by Blood Bank A
        DonationEvent eventA = new DonationEvent();
        eventA.setBloodBankId(bloodBankA.getId());
        eventA.setCreatedBy(staffBankA.getId());
        eventA.setTitle("Camp Alpha");
        eventA.setEventType(DonationEventType.BLOOD_DONATION_CAMP);
        eventA.setStatus(DonationEventStatus.PUBLISHED);
        eventA.setVenueName("Town Hall");
        eventA.setAddress("Hall St");
        eventA.setCity("Mumbai");
        eventA.setState("Maharashtra");
        eventA.setPostalCode("400001");
        eventA.setLatitude(18.9220);
        eventA.setLongitude(72.8347);
        eventA.setStartAt(Instant.now());
        eventA.setEndAt(Instant.now().plusSeconds(28800));
        eventA.setRegistrationOpenAt(Instant.now().minusSeconds(3600));
        eventA.setRegistrationCloseAt(Instant.now().plusSeconds(28800));
        eventA.setDonorCapacity(100);
        eventA = donationEventRepository.save(eventA);

        // Register donor 1 for event
        DonationEventRegistration reg = new DonationEventRegistration();
        reg.setEventId(eventA.getId());
        reg.setDonorUserId(donor1.getId());
        reg.setStatus(DonationEventRegistrationStatus.REGISTERED);
        registrationRepository.save(reg);

        // Staff at Blood Bank B attempts to record verified donation for Bank A's event -> FORBIDDEN
        setSecurityContext(staffBankB.getId(), "ROLE_BLOODBANK");
        RecordVerifiedDonationRequest recordReq = new RecordVerifiedDonationRequest(
                donor1.getId(),
                DonationSourceType.DONATION_EVENT,
                null,
                eventA.getId(),
                LocalDate.now(),
                "Illicit verification attempt"
        );

        assertThrows(UnauthorizedSessionAccessException.class, () ->
                donationService.recordVerifiedDonation(staffBankB.getId(), recordReq, "127.0.0.1", "TestClient"));

        // Staff at Blood Bank A can verify
        setSecurityContext(staffBankA.getId(), "ROLE_BLOODBANK");
        DonationDetailDto validDonation = donationService.recordVerifiedDonation(staffBankA.getId(), recordReq, "127.0.0.1", "TestClient");
        assertNotNull(validDonation.getId());

        // Create Blood Request
        setSecurityContext(requester.getId(), "ROLE_RECEIVER");
        CreateBloodRequestRequest createReq = new CreateBloodRequestRequest();
        createReq.setBloodGroup(BloodGroup.O_POSITIVE);
        createReq.setUnitsRequired(1);
        createReq.setUrgency(BloodRequestUrgency.NORMAL);
        createReq.setHospitalName("City Clinic");
        createReq.setHospitalAddress("Clinic Rd");
        createReq.setCity("Mumbai");
        createReq.setState("Maharashtra");
        createReq.setPostalCode("400001");
        createReq.setLatitude(18.9220);
        createReq.setLongitude(72.8347);
        createReq.setRequiredBy(Instant.now().plusSeconds(86400));
        BloodRequestDetailDto reqDto = bloodRequestService.createRequest(createReq, "127.0.0.1", "TestClient");

        // Staff at Blood Bank B attempts to create fulfillment using Bank A's event donation -> FORBIDDEN
        setSecurityContext(staffBankB.getId(), "ROLE_BLOODBANK");
        assertThrows(UnauthorizedSessionAccessException.class, () ->
                fulfillmentService.createFulfillment(
                        new CreateFulfillmentRequest(reqDto.getId(), validDonation.getId(), 1, "Cross-bank allocation"),
                        "127.0.0.1", "TestClient"
                )
        );

        // Donor attempts clinical fulfillment operation -> FORBIDDEN
        setSecurityContext(staffBankA.getId(), "ROLE_BLOODBANK");
        FulfillmentDto fDto = fulfillmentService.createFulfillment(
                new CreateFulfillmentRequest(reqDto.getId(), validDonation.getId(), 1, "Bank A allocation"),
                "127.0.0.1", "TestClient"
        );

        setSecurityContext(donor1.getId(), "ROLE_DONOR");
        assertThrows(UnauthorizedSessionAccessException.class, () ->
                fulfillmentService.startFulfillment(fDto.getId(), "127.0.0.1", "TestClient"));
    }

    @Test
    @DisplayName("Transactional Integrity: Atomicity across BloodRequest units and Fulfillment status on error")
    void testTransactionalRollbackIntegrity() {
        BloodRequest req = new BloodRequest();
        req.setRequesterUserId(requester.getId());
        req.setBloodGroup(BloodGroup.O_POSITIVE);
        req.setUnitsRequired(1);
        req.setUnitsFulfilled(0);
        req.setStatus(BloodRequestStatus.OPEN);
        req.setHospitalName("City Clinic");
        req.setHospitalAddress("Clinic Rd");
        req.setCity("Mumbai");
        req.setState("Maharashtra");
        req.setPostalCode("400001");
        req.setLatitude(18.9220);
        req.setLongitude(72.8347);
        req.setRequiredBy(Instant.now().plusSeconds(86400));
        BloodRequest savedReq = bloodRequestRepository.save(req);

        Donation d = new Donation(donor1.getId(), DonationSourceType.BLOOD_REQUEST, savedReq.getId(), null, LocalDate.now(), "Notes");
        d.setVerificationStatus(DonationVerificationStatus.VERIFIED);
        d = donationRepository.save(d);

        setSecurityContext(staffBankA.getId(), "ROLE_BLOODBANK");
        FulfillmentDto fDto = fulfillmentService.createFulfillment(
                new CreateFulfillmentRequest(savedReq.getId(), d.getId(), 1, "Notes"),
                "127.0.0.1", "TestClient"
        );

        // Cancel the BloodRequest in the database directly
        savedReq.setStatus(BloodRequestStatus.CANCELLED);
        bloodRequestRepository.save(savedReq);

        // Starting fulfillment on CANCELLED request must fail with ValidationException
        assertThrows(ValidationException.class, () ->
                fulfillmentService.startFulfillment(fDto.getId(), "127.0.0.1", "TestClient"));

        // Fulfillment remains in READY status (not corrupted to IN_PROGRESS)
        Fulfillment unmodifiedF = fulfillmentRepository.findById(fDto.getId()).orElseThrow();
        assertEquals(FulfillmentStatus.READY, unmodifiedF.getStatus());
    }

    @Test
    @DisplayName("Concurrency: Request cancellation racing with fulfillment completion serializes cleanly")
    void testCancellationRacingWithFulfillmentSerializesCleanly() throws Exception {
        BloodRequest req = new BloodRequest();
        req.setRequesterUserId(requester.getId());
        req.setBloodGroup(BloodGroup.O_POSITIVE);
        req.setUnitsRequired(1);
        req.setUnitsFulfilled(0);
        req.setStatus(BloodRequestStatus.OPEN);
        req.setHospitalName("City Clinic");
        req.setHospitalAddress("Clinic Rd");
        req.setCity("Mumbai");
        req.setState("Maharashtra");
        req.setPostalCode("400001");
        req.setLatitude(18.9220);
        req.setLongitude(72.8347);
        req.setRequiredBy(Instant.now().plusSeconds(86400));
        BloodRequest savedReq = bloodRequestRepository.save(req);

        Donation d = new Donation(donor1.getId(), DonationSourceType.BLOOD_REQUEST, savedReq.getId(), null, LocalDate.now(), "Notes");
        d.setVerificationStatus(DonationVerificationStatus.VERIFIED);
        d = donationRepository.save(d);

        setSecurityContext(staffBankA.getId(), "ROLE_BLOODBANK");
        FulfillmentDto fDto = fulfillmentService.createFulfillment(
                new CreateFulfillmentRequest(savedReq.getId(), d.getId(), 1, "Notes"),
                "127.0.0.1", "TestClient"
        );
        fulfillmentService.startFulfillment(fDto.getId(), "127.0.0.1", "TestClient");

        final UUID targetRequestId = savedReq.getId();
        final UUID targetFulfillmentId = fDto.getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);

        // Thread 1: Requester tries to cancel request
        Future<Boolean> cancelFuture = executor.submit(() -> {
            setSecurityContext(requester.getId(), "ROLE_RECEIVER");
            barrier.await();
            try {
                bloodRequestService.cancelRequest(targetRequestId, new CancelBloodRequestRequest("Cancelled"), "127.0.0.1", "TestClient");
                return true;
            } catch (Exception ex) {
                return false;
            } finally {
                clearSecurityContext();
            }
        });

        // Thread 2: Staff tries to complete fulfillment
        Future<Boolean> completeFuture = executor.submit(() -> {
            setSecurityContext(staffBankA.getId(), "ROLE_BLOODBANK");
            barrier.await();
            try {
                fulfillmentService.completeFulfillment(targetFulfillmentId, "127.0.0.1", "TestClient");
                return true;
            } catch (Exception ex) {
                return false;
            } finally {
                clearSecurityContext();
            }
        });

        boolean cancelSuccess = cancelFuture.get(10, TimeUnit.SECONDS);
        boolean completeSuccess = completeFuture.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Exactly one operation succeeds and the other is rejected; no corrupt state
        assertTrue(cancelSuccess ^ completeSuccess, "Exactly one operation must succeed due to pessimistic row serialization");

        BloodRequest finalState = bloodRequestRepository.findById(targetRequestId).orElseThrow();
        if (completeSuccess) {
            assertEquals(BloodRequestStatus.FULFILLED, finalState.getStatus());
            assertEquals(1, finalState.getUnitsFulfilled());
        } else {
            assertEquals(BloodRequestStatus.CANCELLED, finalState.getStatus());
            assertEquals(0, finalState.getUnitsFulfilled());
        }
    }
}
