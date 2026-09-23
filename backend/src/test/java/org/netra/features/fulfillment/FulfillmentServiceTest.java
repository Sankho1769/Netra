package org.netra.features.fulfillment;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.netra.core.audit.AuditService;
import org.netra.core.audit.EligibilityAuditLogRepository;
import org.netra.core.audit.SecurityAuditLogRepository;
import org.netra.core.exception.ResourceNotFoundException;
import org.netra.core.exception.UnauthorizedSessionAccessException;
import org.netra.core.exception.ValidationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.netra.features.bloodbank.repository.BloodBankAccountRepository;
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
import org.netra.features.fulfillment.dto.CancelFulfillmentRequest;
import org.netra.features.fulfillment.dto.CreateFulfillmentRequest;
import org.netra.features.fulfillment.dto.FailFulfillmentRequest;
import org.netra.features.fulfillment.dto.FulfillmentDto;
import org.netra.features.fulfillment.entity.Fulfillment;
import org.netra.features.fulfillment.entity.FulfillmentStatus;
import org.netra.features.fulfillment.repository.FulfillmentRepository;
import org.netra.features.fulfillment.service.FulfillmentAuthorizationService;
import org.netra.features.fulfillment.service.FulfillmentService;
import org.netra.features.matching.rules.BloodCompatibilityMatrix;
import org.netra.features.user.repository.UserRepository;
import org.netra.features.user.entity.User;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Fulfillment Service Domain & Accounting Tests")
class FulfillmentServiceTest {

    @Mock private FulfillmentRepository fulfillmentRepository;
    @Mock private BloodRequestRepository bloodRequestRepository;
    @Mock private DonationRepository donationRepository;
    @Mock private DonorProfileRepository donorProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private BloodBankAccountRepository bloodBankAccountRepository;
    @Mock private EligibilityAuditLogRepository eligibilityAuditLogRepository;
    @Mock private SecurityAuditLogRepository securityAuditLogRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private FulfillmentAuthorizationService authorizationService;
    private BloodCompatibilityMatrix compatibilityMatrix;
    private AuditService auditService;
    private FulfillmentService fulfillmentService;

    private final UUID staffUserId = UUID.randomUUID();
    private final UUID requesterUserId = UUID.randomUUID();
    private final UUID donorUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        authorizationService = new FulfillmentAuthorizationService(userRepository, bloodBankAccountRepository);
        compatibilityMatrix = new BloodCompatibilityMatrix();
        auditService = new AuditService(eligibilityAuditLogRepository, securityAuditLogRepository);

        User staffUser = new User("Staff", "staff@netra.org", "+919830000001", "pass", java.util.Set.of(org.netra.features.user.entity.UserRole.ROLE_ADMIN));
        staffUser.setId(staffUserId);
        staffUser.setStatus(org.netra.features.user.entity.UserStatus.ACTIVE);
        lenient().when(userRepository.findById(staffUserId)).thenReturn(Optional.of(staffUser));

        fulfillmentService = new FulfillmentService(
                fulfillmentRepository,
                bloodRequestRepository,
                donationRepository,
                donorProfileRepository,
                userRepository,
                authorizationService,
                compatibilityMatrix,
                auditService,
                eventPublisher
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                staffUserId.toString(),
                null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_BLOODBANK"))
        ));
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private BloodRequest createMockBloodRequest(UUID id, BloodGroup bg, int unitsRequired, int unitsFulfilled, BloodRequestStatus status) {
        BloodRequest req = new BloodRequest();
        req.setId(id);
        req.setRequesterUserId(requesterUserId);
        req.setBloodGroup(bg);
        req.setUnitsRequired(unitsRequired);
        req.setUnitsFulfilled(unitsFulfilled);
        req.setStatus(status);
        req.setHospitalName("City General Hospital");
        return req;
    }

    private Donation createMockDonation(UUID id, UUID donorId, DonationVerificationStatus status) {
        Donation d = new Donation(donorId, DonationSourceType.BLOOD_REQUEST, UUID.randomUUID(), null, LocalDate.now(), "Notes");
        d.setId(id);
        d.setVerificationStatus(status);
        return d;
    }

    private DonorProfile createMockDonorProfile(UUID donorId, BloodGroup bg) {
        DonorProfile p = new DonorProfile();
        p.setUserId(donorId);
        p.setBloodGroup(bg);
        return p;
    }

    @Test
    @DisplayName("Create: Successfully creates fulfillment in READY status")
    void createFulfillmentSuccess() {
        UUID reqId = UUID.randomUUID();
        UUID donId = UUID.randomUUID();

        BloodRequest req = createMockBloodRequest(reqId, BloodGroup.A_POSITIVE, 3, 0, BloodRequestStatus.OPEN);
        Donation donation = createMockDonation(donId, donorUserId, DonationVerificationStatus.VERIFIED);
        DonorProfile profile = createMockDonorProfile(donorUserId, BloodGroup.O_NEGATIVE); // O- is universal donor

        when(bloodRequestRepository.findByIdForUpdate(reqId)).thenReturn(Optional.of(req));
        when(donationRepository.findById(donId)).thenReturn(Optional.of(donation));
        when(fulfillmentRepository.findActiveByDonationId(donId)).thenReturn(Optional.empty());
        when(donorProfileRepository.findByUserId(donorUserId)).thenReturn(Optional.of(profile));
        when(fulfillmentRepository.sumReservedUnitsForBloodRequest(reqId)).thenReturn(0);
        when(fulfillmentRepository.save(any(Fulfillment.class))).thenAnswer(i -> {
            Fulfillment f = i.getArgument(0);
            f.setId(UUID.randomUUID());
            return f;
        });

        CreateFulfillmentRequest request = new CreateFulfillmentRequest(reqId, donId, 1, "Urgent delivery");
        FulfillmentDto result = fulfillmentService.createFulfillment(request, "127.0.0.1", "TestAgent");

        assertNotNull(result);
        assertEquals(FulfillmentStatus.READY, result.getStatus());
        assertEquals(1, result.getUnits());
        assertEquals(reqId, result.getBloodRequestId());
        assertEquals(donId, result.getDonationId());

        verify(securityAuditLogRepository).save(argThat(l -> l.getEventType().equals("FULFILLMENT_CREATED") && l.getUserId().equals(staffUserId)));
        verify(eventPublisher).publishEvent(any(org.netra.features.fulfillment.event.FulfillmentCreatedEvent.class));
    }

    @Test
    @DisplayName("Create: Rejects if Donation is not VERIFIED")
    void createFulfillmentRejectsUnverifiedDonation() {
        UUID reqId = UUID.randomUUID();
        UUID donId = UUID.randomUUID();

        BloodRequest req = createMockBloodRequest(reqId, BloodGroup.A_POSITIVE, 2, 0, BloodRequestStatus.OPEN);
        Donation donation = createMockDonation(donId, donorUserId, DonationVerificationStatus.PENDING_VERIFICATION);

        when(bloodRequestRepository.findByIdForUpdate(reqId)).thenReturn(Optional.of(req));
        when(donationRepository.findById(donId)).thenReturn(Optional.of(donation));

        CreateFulfillmentRequest request = new CreateFulfillmentRequest(reqId, donId, 1, "Notes");
        ValidationException ex = assertThrows(ValidationException.class, () ->
                fulfillmentService.createFulfillment(request, "127.0.0.1", "TestAgent"));

        assertTrue(ex.getMessage().contains("Only VERIFIED donations may be used"));
    }

    @Test
    @DisplayName("Create: Rejects if Donation already active (double-consumption prevention)")
    void createFulfillmentRejectsAlreadyActiveDonation() {
        UUID reqId = UUID.randomUUID();
        UUID donId = UUID.randomUUID();

        BloodRequest req = createMockBloodRequest(reqId, BloodGroup.A_POSITIVE, 2, 0, BloodRequestStatus.OPEN);
        Donation donation = createMockDonation(donId, donorUserId, DonationVerificationStatus.VERIFIED);
        Fulfillment activeFulfillment = new Fulfillment(UUID.randomUUID(), donId, 1, staffUserId, "Already active");

        when(bloodRequestRepository.findByIdForUpdate(reqId)).thenReturn(Optional.of(req));
        when(donationRepository.findById(donId)).thenReturn(Optional.of(donation));
        when(fulfillmentRepository.findActiveByDonationId(donId)).thenReturn(Optional.of(activeFulfillment));

        CreateFulfillmentRequest request = new CreateFulfillmentRequest(reqId, donId, 1, "Notes");
        ValidationException ex = assertThrows(ValidationException.class, () ->
                fulfillmentService.createFulfillment(request, "127.0.0.1", "TestAgent"));

        assertTrue(ex.getMessage().contains("already been consumed or reserved"));
    }

    @Test
    @DisplayName("Create: Rejects if blood group is incompatible")
    void createFulfillmentRejectsIncompatibleBloodGroup() {
        UUID reqId = UUID.randomUUID();
        UUID donId = UUID.randomUUID();

        // Recipient is O_NEGATIVE (can only receive from O_NEGATIVE)
        BloodRequest req = createMockBloodRequest(reqId, BloodGroup.O_NEGATIVE, 2, 0, BloodRequestStatus.OPEN);
        Donation donation = createMockDonation(donId, donorUserId, DonationVerificationStatus.VERIFIED);
        // Donor is A_POSITIVE (incompatible with O-)
        DonorProfile profile = createMockDonorProfile(donorUserId, BloodGroup.A_POSITIVE);

        when(bloodRequestRepository.findByIdForUpdate(reqId)).thenReturn(Optional.of(req));
        when(donationRepository.findById(donId)).thenReturn(Optional.of(donation));
        when(fulfillmentRepository.findActiveByDonationId(donId)).thenReturn(Optional.empty());
        when(donorProfileRepository.findByUserId(donorUserId)).thenReturn(Optional.of(profile));

        CreateFulfillmentRequest request = new CreateFulfillmentRequest(reqId, donId, 1, "Notes");
        ValidationException ex = assertThrows(ValidationException.class, () ->
                fulfillmentService.createFulfillment(request, "127.0.0.1", "TestAgent"));

        assertTrue(ex.getMessage().contains("is incompatible with requested blood group"));
    }

    @Test
    @DisplayName("Create: Rejects if requested units exceed remaining request quantity")
    void createFulfillmentRejectsExcessiveQuantity() {
        UUID reqId = UUID.randomUUID();
        UUID donId = UUID.randomUUID();

        // 3 units required, 2 already fulfilled, 0 reserved -> only 1 remaining!
        BloodRequest req = createMockBloodRequest(reqId, BloodGroup.A_POSITIVE, 3, 2, BloodRequestStatus.OPEN);
        Donation donation = createMockDonation(donId, donorUserId, DonationVerificationStatus.VERIFIED);
        DonorProfile profile = createMockDonorProfile(donorUserId, BloodGroup.A_POSITIVE);

        when(bloodRequestRepository.findByIdForUpdate(reqId)).thenReturn(Optional.of(req));
        when(donationRepository.findById(donId)).thenReturn(Optional.of(donation));
        when(fulfillmentRepository.findActiveByDonationId(donId)).thenReturn(Optional.empty());
        when(donorProfileRepository.findByUserId(donorUserId)).thenReturn(Optional.of(profile));
        when(fulfillmentRepository.sumReservedUnitsForBloodRequest(reqId)).thenReturn(0);

        // Attempt to claim 2 units when only 1 is remaining
        CreateFulfillmentRequest request = new CreateFulfillmentRequest(reqId, donId, 2, "Notes");
        ValidationException ex = assertThrows(ValidationException.class, () ->
                fulfillmentService.createFulfillment(request, "127.0.0.1", "TestAgent"));

        assertTrue(ex.getMessage().contains("exceeds the blood request's remaining unreserved quantity"));
    }

    @Test
    @DisplayName("Lifecycle: startFulfillment transitions READY to IN_PROGRESS")
    void startFulfillmentTransitionsToInProgress() {
        UUID fulfillmentId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();
        UUID donId = UUID.randomUUID();

        Fulfillment f = new Fulfillment(reqId, donId, 1, staffUserId, "Notes");
        f.setId(fulfillmentId);

        BloodRequest req = createMockBloodRequest(reqId, BloodGroup.B_POSITIVE, 2, 0, BloodRequestStatus.OPEN);
        Donation don = createMockDonation(donId, donorUserId, DonationVerificationStatus.VERIFIED);

        when(fulfillmentRepository.findById(fulfillmentId)).thenReturn(Optional.of(f));
        when(fulfillmentRepository.save(any(Fulfillment.class))).thenAnswer(i -> i.getArgument(0));
        when(bloodRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(donationRepository.findById(donId)).thenReturn(Optional.of(don));

        FulfillmentDto result = fulfillmentService.startFulfillment(fulfillmentId, "127.0.0.1", "TestAgent");

        assertEquals(FulfillmentStatus.IN_PROGRESS, result.getStatus());
        assertNotNull(result.getStartedAt());
        assertEquals(staffUserId, result.getStartedByUserId());

        verify(securityAuditLogRepository).save(argThat(l -> l.getEventType().equals("FULFILLMENT_STARTED") && l.getUserId().equals(staffUserId)));
        verify(eventPublisher).publishEvent(any(org.netra.features.fulfillment.event.FulfillmentStartedEvent.class));
    }

    @Test
    @DisplayName("Quantity Accounting: Partial fulfillment keeps BloodRequest OPEN")
    void completePartialFulfillmentKeepsBloodRequestOpen() {
        UUID fulfillmentId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();
        UUID donId = UUID.randomUUID();

        // 3 units required, 0 fulfilled
        BloodRequest req = createMockBloodRequest(reqId, BloodGroup.B_POSITIVE, 3, 0, BloodRequestStatus.OPEN);
        Donation don = createMockDonation(donId, donorUserId, DonationVerificationStatus.VERIFIED);

        Fulfillment f = new Fulfillment(reqId, donId, 1, staffUserId, "Notes");
        f.setId(fulfillmentId);
        f.start(staffUserId, Instant.now());

        when(fulfillmentRepository.findById(fulfillmentId)).thenReturn(Optional.of(f));
        when(bloodRequestRepository.findByIdForUpdate(reqId)).thenReturn(Optional.of(req));
        when(fulfillmentRepository.save(any(Fulfillment.class))).thenAnswer(i -> i.getArgument(0));
        when(donationRepository.findById(donId)).thenReturn(Optional.of(don));

        FulfillmentDto result = fulfillmentService.completeFulfillment(fulfillmentId, "127.0.0.1", "TestAgent");

        assertEquals(FulfillmentStatus.FULFILLED, result.getStatus());
        // Units fulfilled increased from 0 to 1
        assertEquals(1, req.getUnitsFulfilled());
        // BloodRequest remains OPEN because 1 < 3
        assertEquals(BloodRequestStatus.OPEN, req.getStatus());
        assertNull(req.getFulfilledAt());

        verify(bloodRequestRepository).save(req);
        verify(securityAuditLogRepository).save(argThat(l -> l.getEventType().equals("FULFILLMENT_COMPLETED") && l.getUserId().equals(staffUserId)));
    }

    @Test
    @DisplayName("Quantity Accounting: Final fulfillment atomically transitions BloodRequest to FULFILLED")
    void completeFinalFulfillmentCompletesBloodRequest() {
        UUID fulfillmentId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();
        UUID donId = UUID.randomUUID();

        // 2 units required, 1 already fulfilled -> this 1-unit fulfillment completes it!
        BloodRequest req = createMockBloodRequest(reqId, BloodGroup.B_POSITIVE, 2, 1, BloodRequestStatus.OPEN);
        Donation don = createMockDonation(donId, donorUserId, DonationVerificationStatus.VERIFIED);

        Fulfillment f = new Fulfillment(reqId, donId, 1, staffUserId, "Notes");
        f.setId(fulfillmentId);
        f.start(staffUserId, Instant.now());

        when(fulfillmentRepository.findById(fulfillmentId)).thenReturn(Optional.of(f));
        when(bloodRequestRepository.findByIdForUpdate(reqId)).thenReturn(Optional.of(req));
        when(fulfillmentRepository.save(any(Fulfillment.class))).thenAnswer(i -> i.getArgument(0));
        when(donationRepository.findById(donId)).thenReturn(Optional.of(don));

        FulfillmentDto result = fulfillmentService.completeFulfillment(fulfillmentId, "127.0.0.1", "TestAgent");

        assertEquals(FulfillmentStatus.FULFILLED, result.getStatus());
        // Units fulfilled reached units required (2 == 2)
        assertEquals(2, req.getUnitsFulfilled());
        assertEquals(BloodRequestStatus.FULFILLED, req.getStatus());
        assertNotNull(req.getFulfilledAt());
        assertEquals(staffUserId, req.getFulfilledBy());

        verify(bloodRequestRepository).save(req);
        verify(securityAuditLogRepository).save(argThat(l -> l.getEventType().equals("FULFILLMENT_COMPLETED") && l.getUserId().equals(staffUserId)));
    }

    @Test
    @DisplayName("Lifecycle: failFulfillment transitions to FAILED with reason")
    void failFulfillmentTransitionsToFailed() {
        UUID fulfillmentId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();
        UUID donId = UUID.randomUUID();

        BloodRequest req = createMockBloodRequest(reqId, BloodGroup.O_POSITIVE, 1, 0, BloodRequestStatus.OPEN);
        Donation don = createMockDonation(donId, donorUserId, DonationVerificationStatus.VERIFIED);

        Fulfillment f = new Fulfillment(reqId, donId, 1, staffUserId, "Notes");
        f.setId(fulfillmentId);
        f.start(staffUserId, Instant.now());

        when(fulfillmentRepository.findById(fulfillmentId)).thenReturn(Optional.of(f));
        when(fulfillmentRepository.save(any(Fulfillment.class))).thenAnswer(i -> i.getArgument(0));
        when(bloodRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(donationRepository.findById(donId)).thenReturn(Optional.of(don));

        FailFulfillmentRequest failReq = new FailFulfillmentRequest("Unit damaged in transport", "Bag leaked");
        FulfillmentDto result = fulfillmentService.failFulfillment(fulfillmentId, failReq, "127.0.0.1", "TestAgent");

        assertEquals(FulfillmentStatus.FAILED, result.getStatus());
        assertEquals("Unit damaged in transport", result.getFailureReason());
        assertNotNull(result.getFailedAt());

        verify(securityAuditLogRepository).save(argThat(l -> l.getEventType().equals("FULFILLMENT_FAILED") && l.getUserId().equals(staffUserId)));
    }

    @Test
    @DisplayName("Lifecycle: cancelFulfillment transitions to CANCELLED with reason")
    void cancelFulfillmentTransitionsToCancelled() {
        UUID fulfillmentId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();
        UUID donId = UUID.randomUUID();

        BloodRequest req = createMockBloodRequest(reqId, BloodGroup.O_POSITIVE, 1, 0, BloodRequestStatus.OPEN);
        Donation don = createMockDonation(donId, donorUserId, DonationVerificationStatus.VERIFIED);

        Fulfillment f = new Fulfillment(reqId, donId, 1, staffUserId, "Notes");
        f.setId(fulfillmentId);

        when(fulfillmentRepository.findById(fulfillmentId)).thenReturn(Optional.of(f));
        when(fulfillmentRepository.save(any(Fulfillment.class))).thenAnswer(i -> i.getArgument(0));
        when(bloodRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(donationRepository.findById(donId)).thenReturn(Optional.of(don));

        CancelFulfillmentRequest cancelReq = new CancelFulfillmentRequest("Alternative blood source found", null);
        FulfillmentDto result = fulfillmentService.cancelFulfillment(fulfillmentId, cancelReq, "127.0.0.1", "TestAgent");

        assertEquals(FulfillmentStatus.CANCELLED, result.getStatus());
        assertEquals("Alternative blood source found", result.getCancellationReason());
        assertNotNull(result.getCancelledAt());

        verify(securityAuditLogRepository).save(argThat(l -> l.getEventType().equals("FULFILLMENT_CANCELLED") && l.getUserId().equals(staffUserId)));
    }
}
