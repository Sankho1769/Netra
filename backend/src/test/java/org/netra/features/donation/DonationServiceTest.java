package org.netra.features.donation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.netra.core.audit.AuditService;
import org.netra.core.exception.DuplicateResourceException;
import org.netra.core.exception.ResourceNotFoundException;
import org.netra.core.exception.ValidationException;
import org.netra.features.bloodbank.repository.BloodBankAccountRepository;
import org.netra.features.bloodrequest.entity.BloodRequest;
import org.netra.features.bloodrequest.entity.BloodRequestStatus;
import org.netra.features.bloodrequest.repository.BloodRequestRepository;
import org.netra.features.donation.dto.*;
import org.netra.features.donation.entity.Donation;
import org.netra.features.donation.entity.DonationSourceType;
import org.netra.features.donation.entity.DonationVerificationStatus;
import org.netra.features.donation.event.DonationRejectedEvent;
import org.netra.features.donation.event.DonationSubmittedEvent;
import org.netra.features.donation.event.DonationVerifiedEvent;
import org.netra.features.donation.repository.DonationRepository;
import org.netra.features.donation.service.DonationAuthorizationService;
import org.netra.features.donation.service.DonationService;
import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.donor.entity.DonorProfile;
import org.netra.features.donor.repository.DonorProfileRepository;
import org.netra.features.events.entity.DonationEvent;
import org.netra.features.events.entity.DonationEventRegistration;
import org.netra.features.events.entity.DonationEventRegistrationStatus;
import org.netra.features.events.entity.DonationEventStatus;
import org.netra.features.events.repository.DonationEventRegistrationRepository;
import org.netra.features.events.repository.DonationEventRepository;
import org.netra.features.matching.entity.DonorMatch;
import org.netra.features.matching.entity.MatchStatus;
import org.netra.features.matching.repository.DonorMatchRepository;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.entity.UserStatus;
import org.netra.features.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DonationServiceTest {

    @Mock private DonationRepository donationRepository;
    @Mock private DonorProfileRepository donorProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private BloodRequestRepository bloodRequestRepository;
    @Mock private DonorMatchRepository donorMatchRepository;
    @Mock private DonationEventRepository donationEventRepository;
    @Mock private DonationEventRegistrationRepository registrationRepository;
    @Mock private BloodBankAccountRepository bloodBankAccountRepository;
    @Mock private org.netra.core.audit.EligibilityAuditLogRepository eligibilityAuditLogRepository;
    @Mock private org.netra.core.audit.SecurityAuditLogRepository securityAuditLogRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private DonationAuthorizationService authorizationService;
    private AuditService auditService;
    private DonationService donationService;

    private UUID donorUserId;
    private UUID verifierUserId;
    private UUID adminUserId;
    private UUID bloodRequestId;
    private UUID eventId;
    private User donorUser;
    private User verifierUser;
    private User adminUser;
    private DonorProfile donorProfile;
    private BloodRequest bloodRequest;
    private DonationEvent donationEvent;

    @BeforeEach
    void setUp() {
        authorizationService = new DonationAuthorizationService(
                userRepository,
                bloodBankAccountRepository,
                donationEventRepository
        );

        auditService = new AuditService(
                eligibilityAuditLogRepository,
                securityAuditLogRepository
        );

        donationService = new DonationService(
                donationRepository,
                donorProfileRepository,
                userRepository,
                bloodRequestRepository,
                donorMatchRepository,
                donationEventRepository,
                registrationRepository,
                authorizationService,
                auditService,
                eventPublisher
        );

        donorUserId = UUID.randomUUID();
        verifierUserId = UUID.randomUUID();
        adminUserId = UUID.randomUUID();
        bloodRequestId = UUID.randomUUID();
        eventId = UUID.randomUUID();

        donorUser = new User("Donor User", "donor@netra.org", "+919876543210", "hash", new HashSet<>(Set.of(UserRole.ROLE_DONOR)));
        donorUser.setId(donorUserId);

        verifierUser = new User("Verifier User", "verifier@netra.org", "+919876543212", "hash", new HashSet<>(Set.of(UserRole.ROLE_ADMIN)));
        verifierUser.setId(verifierUserId);

        adminUser = new User("Admin User", "admin@netra.org", "+919876543211", "hash", new HashSet<>(Set.of(UserRole.ROLE_ADMIN)));
        adminUser.setId(adminUserId);

        donorProfile = new DonorProfile();
        donorProfile.setUserId(donorUserId);
        donorProfile.setBloodGroup(BloodGroup.O_POSITIVE);
        donorProfile.setLastDonationDate(null);

        bloodRequest = new BloodRequest();
        bloodRequest.setId(bloodRequestId);
        bloodRequest.setBloodGroup(BloodGroup.O_POSITIVE);
        bloodRequest.setHospitalName("Metro Hospital");
        bloodRequest.setCity("Kolkata");
        bloodRequest.setStatus(BloodRequestStatus.OPEN);

        donationEvent = new DonationEvent();
        donationEvent.setId(eventId);
        donationEvent.setTitle("Rotary Blood Camp");
        donationEvent.setVenueName("Town Hall");
        donationEvent.setCity("Kolkata");
        donationEvent.setStatus(DonationEventStatus.PUBLISHED);
    }

    @Test
    @DisplayName("Claim: Submitting valid blood request claim creates PENDING_VERIFICATION donation")
    void testCreateClaim_BloodRequest_Success() {
        when(userRepository.findById(donorUserId)).thenReturn(Optional.of(donorUser));
        when(donorProfileRepository.existsByUserId(donorUserId)).thenReturn(true);
        when(bloodRequestRepository.findById(bloodRequestId)).thenReturn(Optional.of(bloodRequest));
        when(donationRepository.existsByDonorUserIdAndBloodRequestIdAndVerificationStatusIn(eq(donorUserId), eq(bloodRequestId), any()))
                .thenReturn(false);

        DonorMatch match = new DonorMatch(bloodRequestId, donorUserId, Instant.now().plusSeconds(3600));
        match.accept(Instant.now());
        when(donorMatchRepository.findByBloodRequestIdAndDonorUserId(bloodRequestId, donorUserId))
                .thenReturn(Optional.of(match));

        when(donationRepository.save(any(Donation.class))).thenAnswer(inv -> {
            Donation d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        CreateDonationClaimRequest req = new CreateDonationClaimRequest(
                DonationSourceType.BLOOD_REQUEST,
                bloodRequestId,
                null,
                LocalDate.now().minusDays(1),
                "Donated at Metro Hospital ward 4"
        );

        DonationDetailDto result = donationService.createClaim(donorUserId, req, "127.0.0.1", "JUnit");

        assertNotNull(result);
        assertEquals(DonationSourceType.BLOOD_REQUEST, result.getSourceType());
        assertEquals(DonationVerificationStatus.PENDING_VERIFICATION, result.getVerificationStatus());
        assertEquals(donorUserId, result.getDonorUserId());

        verify(donationRepository).save(any(Donation.class));
        verify(securityAuditLogRepository).save(any());
        verify(eventPublisher).publishEvent(any(DonationSubmittedEvent.class));
    }

    @Test
    @DisplayName("Claim: Submitting valid event claim creates PENDING_VERIFICATION donation")
    void testCreateClaim_DonationEvent_Success() {
        when(userRepository.findById(donorUserId)).thenReturn(Optional.of(donorUser));
        when(donorProfileRepository.existsByUserId(donorUserId)).thenReturn(true);
        when(donationEventRepository.findById(eventId)).thenReturn(Optional.of(donationEvent));
        when(donationRepository.existsByDonorUserIdAndDonationEventIdAndVerificationStatusIn(eq(donorUserId), eq(eventId), any()))
                .thenReturn(false);

        DonationEventRegistration reg = new DonationEventRegistration(eventId, donorUserId, DonationEventRegistrationStatus.CHECKED_IN);
        when(registrationRepository.findByEventIdAndDonorUserId(eventId, donorUserId)).thenReturn(Optional.of(reg));

        when(donationRepository.save(any(Donation.class))).thenAnswer(inv -> {
            Donation d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        CreateDonationClaimRequest req = new CreateDonationClaimRequest(
                DonationSourceType.DONATION_EVENT,
                null,
                eventId,
                LocalDate.now(),
                "Attended rotary camp"
        );

        DonationDetailDto result = donationService.createClaim(donorUserId, req, "127.0.0.1", "JUnit");

        assertNotNull(result);
        assertEquals(DonationSourceType.DONATION_EVENT, result.getSourceType());
        assertEquals(DonationVerificationStatus.PENDING_VERIFICATION, result.getVerificationStatus());

        verify(eventPublisher).publishEvent(any(DonationSubmittedEvent.class));
    }

    @Test
    @DisplayName("Claim: Future donation date is rejected")
    void testCreateClaim_FutureDate_ThrowsValidationException() {
        when(userRepository.findById(donorUserId)).thenReturn(Optional.of(donorUser));
        when(donorProfileRepository.existsByUserId(donorUserId)).thenReturn(true);

        CreateDonationClaimRequest req = new CreateDonationClaimRequest(
                DonationSourceType.BLOOD_REQUEST,
                bloodRequestId,
                null,
                LocalDate.now().plusDays(2),
                null
        );

        assertThrows(ValidationException.class, () ->
                donationService.createClaim(donorUserId, req, "127.0.0.1", "JUnit"));
    }

    @Test
    @DisplayName("Claim: Blood request match not accepted throws ValidationException")
    void testCreateClaim_MatchNotAccepted_ThrowsValidationException() {
        when(userRepository.findById(donorUserId)).thenReturn(Optional.of(donorUser));
        when(donorProfileRepository.existsByUserId(donorUserId)).thenReturn(true);
        when(bloodRequestRepository.findById(bloodRequestId)).thenReturn(Optional.of(bloodRequest));

        DonorMatch unacceptedMatch = new DonorMatch(bloodRequestId, donorUserId, Instant.now().plusSeconds(3600));
        when(donorMatchRepository.findByBloodRequestIdAndDonorUserId(bloodRequestId, donorUserId))
                .thenReturn(Optional.of(unacceptedMatch));

        CreateDonationClaimRequest req = new CreateDonationClaimRequest(
                DonationSourceType.BLOOD_REQUEST,
                bloodRequestId,
                null,
                LocalDate.now(),
                null
        );

        assertThrows(ValidationException.class, () ->
                donationService.createClaim(donorUserId, req, "127.0.0.1", "JUnit"));
    }

    @Test
    @DisplayName("Claim: Duplicate donation claim for same request throws DuplicateResourceException")
    void testCreateClaim_Duplicate_ThrowsDuplicateResourceException() {
        when(userRepository.findById(donorUserId)).thenReturn(Optional.of(donorUser));
        when(donorProfileRepository.existsByUserId(donorUserId)).thenReturn(true);
        when(bloodRequestRepository.findById(bloodRequestId)).thenReturn(Optional.of(bloodRequest));
        when(donationRepository.existsByDonorUserIdAndBloodRequestIdAndVerificationStatusIn(eq(donorUserId), eq(bloodRequestId), any()))
                .thenReturn(true);

        CreateDonationClaimRequest req = new CreateDonationClaimRequest(
                DonationSourceType.BLOOD_REQUEST,
                bloodRequestId,
                null,
                LocalDate.now(),
                null
        );

        assertThrows(DuplicateResourceException.class, () ->
                donationService.createClaim(donorUserId, req, "127.0.0.1", "JUnit"));
    }

    @Test
    @DisplayName("Verify: Verification transitions status to VERIFIED and updates donor lastDonationDate")
    void testVerifyDonation_Success_UpdatesStatusAndLastDonationDate() {
        UUID donationId = UUID.randomUUID();
        LocalDate donationDate = LocalDate.now().minusDays(2);

        Donation donation = new Donation(
                donorUserId,
                DonationSourceType.BLOOD_REQUEST,
                bloodRequestId,
                null,
                donationDate,
                "Claim notes"
        );
        donation.setId(donationId);

        when(donationRepository.findById(donationId)).thenReturn(Optional.of(donation));
        when(donationRepository.save(any(Donation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(donorProfileRepository.findByUserId(donorUserId)).thenReturn(Optional.of(donorProfile));
        when(userRepository.findById(donorUserId)).thenReturn(Optional.of(donorUser));
        when(userRepository.findById(verifierUserId)).thenReturn(Optional.of(verifierUser));

        VerifyDonationRequest req = new VerifyDonationRequest("Verified via clinical register");

        DonationDetailDto verified = donationService.verifyDonation(donationId, verifierUserId, req, "127.0.0.1", "JUnit");

        assertEquals(DonationVerificationStatus.VERIFIED, verified.getVerificationStatus());
        assertEquals(verifierUserId, verified.getVerifiedByUserId());
        assertNotNull(verified.getVerifiedAt());

        assertEquals(donationDate, donorProfile.getLastDonationDate());
        verify(donorProfileRepository).save(donorProfile);
        verify(securityAuditLogRepository).save(any());
        verify(eventPublisher).publishEvent(any(DonationVerifiedEvent.class));
    }

    @Test
    @DisplayName("Verify: Event donation verification updates event registration to COMPLETED")
    void testVerifyDonation_EventSource_CompletesEventRegistration() {
        UUID donationId = UUID.randomUUID();
        LocalDate donationDate = LocalDate.now();

        Donation donation = new Donation(
                donorUserId,
                DonationSourceType.DONATION_EVENT,
                null,
                eventId,
                donationDate,
                null
        );
        donation.setId(donationId);

        when(donationRepository.findById(donationId)).thenReturn(Optional.of(donation));
        when(donationRepository.save(any(Donation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(donorProfileRepository.findByUserId(donorUserId)).thenReturn(Optional.of(donorProfile));
        when(userRepository.findById(donorUserId)).thenReturn(Optional.of(donorUser));
        when(userRepository.findById(verifierUserId)).thenReturn(Optional.of(verifierUser));

        DonationEventRegistration reg = new DonationEventRegistration(eventId, donorUserId, DonationEventRegistrationStatus.CHECKED_IN);
        when(registrationRepository.findByEventIdAndDonorUserId(eventId, donorUserId)).thenReturn(Optional.of(reg));

        donationService.verifyDonation(donationId, verifierUserId, new VerifyDonationRequest(), "127.0.0.1", "JUnit");

        assertEquals(DonationEventRegistrationStatus.COMPLETED, reg.getStatus());
        assertNotNull(reg.getCompletedAt());
        verify(registrationRepository).save(reg);
    }

    @Test
    @DisplayName("Verify: Re-verifying already VERIFIED donation throws ValidationException")
    void testVerifyDonation_AlreadyVerified_ThrowsValidationException() {
        UUID donationId = UUID.randomUUID();
        Donation donation = new Donation(
                donorUserId,
                DonationSourceType.BLOOD_REQUEST,
                bloodRequestId,
                null,
                LocalDate.now(),
                null
        );
        donation.setId(donationId);
        donation.verify(verifierUserId, Instant.now(), null);

        when(donationRepository.findById(donationId)).thenReturn(Optional.of(donation));
        when(userRepository.findById(verifierUserId)).thenReturn(Optional.of(verifierUser));

        assertThrows(ValidationException.class, () ->
                donationService.verifyDonation(donationId, verifierUserId, new VerifyDonationRequest(), "127.0.0.1", "JUnit"));
    }

    @Test
    @DisplayName("Reject: Rejection transitions status to REJECTED and does NOT update lastDonationDate")
    void testRejectDonation_Success_PreservesLastDonationDate() {
        UUID donationId = UUID.randomUUID();
        Donation donation = new Donation(
                donorUserId,
                DonationSourceType.BLOOD_REQUEST,
                bloodRequestId,
                null,
                LocalDate.now().minusDays(1),
                null
        );
        donation.setId(donationId);

        when(donationRepository.findById(donationId)).thenReturn(Optional.of(donation));
        when(donationRepository.save(any(Donation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(donorUserId)).thenReturn(Optional.of(donorUser));
        when(userRepository.findById(verifierUserId)).thenReturn(Optional.of(verifierUser));

        RejectDonationRequest req = new RejectDonationRequest("Hospital records show donor did not attend");

        DonationDetailDto rejected = donationService.rejectDonation(donationId, verifierUserId, req, "127.0.0.1", "JUnit");

        assertEquals(DonationVerificationStatus.REJECTED, rejected.getVerificationStatus());
        assertEquals("Hospital records show donor did not attend", rejected.getRejectionReason());

        assertNull(donorProfile.getLastDonationDate());
        verify(donorProfileRepository, never()).save(any());
        verify(securityAuditLogRepository).save(any());
        verify(eventPublisher).publishEvent(any(DonationRejectedEvent.class));
    }

    @Test
    @DisplayName("Cancel: Claiming donor can cancel pending claim")
    void testCancelClaim_Success() {
        UUID donationId = UUID.randomUUID();
        Donation donation = new Donation(
                donorUserId,
                DonationSourceType.BLOOD_REQUEST,
                bloodRequestId,
                null,
                LocalDate.now(),
                null
        );
        donation.setId(donationId);

        when(donationRepository.findById(donationId)).thenReturn(Optional.of(donation));
        when(donationRepository.save(any(Donation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(donorUserId)).thenReturn(Optional.of(donorUser));

        DonationDetailDto cancelled = donationService.cancelClaim(donationId, donorUserId, "127.0.0.1", "JUnit");

        assertEquals(DonationVerificationStatus.CANCELLED, cancelled.getVerificationStatus());
        verify(securityAuditLogRepository).save(any());
    }

    @Test
    @DisplayName("Admin Correction: Reverting VERIFIED donation recalculates donor lastDonationDate")
    void testAdminCorrection_RevertingVerified_RecalculatesLastDonationDate() {
        UUID donationId = UUID.randomUUID();
        LocalDate donationDate = LocalDate.now().minusDays(5);
        LocalDate earlierVerifiedDate = LocalDate.now().minusDays(60);

        donorProfile.setLastDonationDate(donationDate);

        Donation donation = new Donation(
                donorUserId,
                DonationSourceType.BLOOD_REQUEST,
                bloodRequestId,
                null,
                donationDate,
                null
        );
        donation.setId(donationId);
        donation.verify(verifierUserId, Instant.now().minusSeconds(3600), null);

        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(adminUser));
        when(donationRepository.findById(donationId)).thenReturn(Optional.of(donation));
        when(donationRepository.save(any(Donation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(donorProfileRepository.findByUserId(donorUserId)).thenReturn(Optional.of(donorProfile));
        when(userRepository.findById(donorUserId)).thenReturn(Optional.of(donorUser));
        when(donationRepository.findLatestVerifiedDonationDate(donorUserId)).thenReturn(Optional.of(earlierVerifiedDate));

        AdminCorrectionRequest req = new AdminCorrectionRequest(
                DonationVerificationStatus.REJECTED,
                "Donor was erroneously marked verified; sample was disqualified"
        );

        DonationDetailDto corrected = donationService.adminCorrection(donationId, adminUserId, req, "127.0.0.1", "JUnit");

        assertEquals(DonationVerificationStatus.REJECTED, corrected.getVerificationStatus());
        assertEquals(earlierVerifiedDate, donorProfile.getLastDonationDate());
        verify(donorProfileRepository).save(donorProfile);
        verify(securityAuditLogRepository).save(any());
    }
}
