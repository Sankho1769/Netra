package org.netra.features.donation.service;

import org.netra.core.audit.AuditService;
import org.netra.core.exception.*;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DonationService {

    private static final Logger log = LoggerFactory.getLogger(DonationService.class);

    private final DonationRepository donationRepository;
    private final DonorProfileRepository donorProfileRepository;
    private final UserRepository userRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final DonorMatchRepository donorMatchRepository;
    private final DonationEventRepository donationEventRepository;
    private final DonationEventRegistrationRepository registrationRepository;
    private final DonationAuthorizationService authorizationService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    public DonationService(
            DonationRepository donationRepository,
            DonorProfileRepository donorProfileRepository,
            UserRepository userRepository,
            BloodRequestRepository bloodRequestRepository,
            DonorMatchRepository donorMatchRepository,
            DonationEventRepository donationEventRepository,
            DonationEventRegistrationRepository registrationRepository,
            DonationAuthorizationService authorizationService,
            AuditService auditService,
            ApplicationEventPublisher eventPublisher) {
        this.donationRepository = donationRepository;
        this.donorProfileRepository = donorProfileRepository;
        this.userRepository = userRepository;
        this.bloodRequestRepository = bloodRequestRepository;
        this.donorMatchRepository = donorMatchRepository;
        this.donationEventRepository = donationEventRepository;
        this.registrationRepository = registrationRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Submits a donation claim from an authenticated donor for review.
     * Status begins in PENDING_VERIFICATION.
     */
    @Transactional
    public DonationDetailDto createClaim(UUID donorUserId, CreateDonationClaimRequest request, String clientIp, String userAgent) {
        validateDonorAccount(donorUserId);
        validateDonationDate(request.getDonationDate());

        if (request.getSourceType() == DonationSourceType.BLOOD_REQUEST) {
            validateBloodRequestDonationEligibility(donorUserId, request.getBloodRequestId(), request.getDonationDate());
        } else if (request.getSourceType() == DonationSourceType.DONATION_EVENT) {
            validateEventDonationEligibility(donorUserId, request.getDonationEventId());
        } else {
            throw new ValidationException("Unsupported donation source type: " + request.getSourceType());
        }

        Donation donation = new Donation(
                donorUserId,
                request.getSourceType(),
                request.getBloodRequestId(),
                request.getDonationEventId(),
                request.getDonationDate(),
                request.getNotes()
        );

        Donation saved = donationRepository.save(donation);

        auditService.logAuthEvent(
                "DONATION_CLAIM_SUBMITTED",
                donorUserId,
                clientIp,
                userAgent,
                "{\"donationId\":\"" + saved.getId() + "\",\"sourceType\":\"" + saved.getSourceType() + "\"}"
        );

        eventPublisher.publishEvent(new DonationSubmittedEvent(
                saved.getId(),
                saved.getDonorUserId(),
                saved.getSourceType(),
                saved.getDonationDate()
        ));

        return buildDetailDto(saved);
    }

    /**
     * Authorized staff directly records and verifies an in-person donation.
     */
    @Transactional
    public DonationDetailDto recordVerifiedDonation(UUID verifierUserId, RecordVerifiedDonationRequest request, String clientIp, String userAgent) {
        validateDonorAccount(request.getDonorUserId());
        validateDonationDate(request.getDonationDate());

        Donation dummyForAuth = new Donation();
        dummyForAuth.setSourceType(request.getSourceType());
        dummyForAuth.setBloodRequestId(request.getBloodRequestId());
        dummyForAuth.setDonationEventId(request.getDonationEventId());
        authorizationService.verifyCanVerifyDonation(verifierUserId, dummyForAuth);

        if (request.getSourceType() == DonationSourceType.BLOOD_REQUEST) {
            validateBloodRequestDonationEligibility(request.getDonorUserId(), request.getBloodRequestId(), request.getDonationDate());
        } else if (request.getSourceType() == DonationSourceType.DONATION_EVENT) {
            validateEventDonationEligibility(request.getDonorUserId(), request.getDonationEventId());
        }

        Instant now = Instant.now();
        Donation donation = new Donation(
                request.getDonorUserId(),
                request.getSourceType(),
                request.getBloodRequestId(),
                request.getDonationEventId(),
                request.getDonationDate(),
                request.getNotes()
        );
        donation.verify(verifierUserId, now, request.getNotes());
        Donation saved = donationRepository.save(donation);

        // Update DonorProfile.lastDonationDate
        updateDonorLastDonationDate(saved.getDonorUserId(), saved.getDonationDate());

        // Update event registration if applicable
        if (saved.getSourceType() == DonationSourceType.DONATION_EVENT) {
            markRegistrationCompleted(saved.getDonationEventId(), saved.getDonorUserId(), now);
        }

        auditService.logAuthEvent(
                "DONATION_RECORDED_VERIFIED",
                verifierUserId,
                clientIp,
                userAgent,
                "{\"donationId\":\"" + saved.getId() + "\",\"donorUserId\":\"" + saved.getDonorUserId() + "\"}"
        );

        eventPublisher.publishEvent(new DonationVerifiedEvent(
                saved.getId(),
                saved.getDonorUserId(),
                verifierUserId,
                saved.getSourceType(),
                saved.getDonationDate()
        ));

        return buildDetailDto(saved);
    }

    /**
     * Authorized staff verifies a pending donation claim.
     */
    @Transactional
    public DonationDetailDto verifyDonation(UUID donationId, UUID verifierUserId, VerifyDonationRequest request, String clientIp, String userAgent) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found with id: " + donationId));

        authorizationService.verifyCanVerifyDonation(verifierUserId, donation);

        Instant now = Instant.now();
        donation.verify(verifierUserId, now, request != null ? request.getNotes() : null);
        Donation saved = donationRepository.save(donation);

        // Update DonorProfile.lastDonationDate
        updateDonorLastDonationDate(saved.getDonorUserId(), saved.getDonationDate());

        // Complete registration if event-based
        if (saved.getSourceType() == DonationSourceType.DONATION_EVENT) {
            markRegistrationCompleted(saved.getDonationEventId(), saved.getDonorUserId(), now);
        }

        auditService.logAuthEvent(
                "DONATION_VERIFIED",
                verifierUserId,
                clientIp,
                userAgent,
                "{\"donationId\":\"" + saved.getId() + "\",\"donorUserId\":\"" + saved.getDonorUserId() + "\"}"
        );

        eventPublisher.publishEvent(new DonationVerifiedEvent(
                saved.getId(),
                saved.getDonorUserId(),
                verifierUserId,
                saved.getSourceType(),
                saved.getDonationDate()
        ));

        return buildDetailDto(saved);
    }

    /**
     * Authorized staff rejects a pending donation claim with a reason.
     */
    @Transactional
    public DonationDetailDto rejectDonation(UUID donationId, UUID verifierUserId, RejectDonationRequest request, String clientIp, String userAgent) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found with id: " + donationId));

        authorizationService.verifyCanVerifyDonation(verifierUserId, donation);

        Instant now = Instant.now();
        donation.reject(verifierUserId, now, request.getRejectionReason());
        Donation saved = donationRepository.save(donation);

        auditService.logAuthEvent(
                "DONATION_REJECTED",
                verifierUserId,
                clientIp,
                userAgent,
                "{\"donationId\":\"" + saved.getId() + "\",\"reason\":\"" + request.getRejectionReason() + "\"}"
        );

        eventPublisher.publishEvent(new DonationRejectedEvent(
                saved.getId(),
                saved.getDonorUserId(),
                verifierUserId,
                saved.getSourceType(),
                request.getRejectionReason()
        ));

        return buildDetailDto(saved);
    }

    /**
     * Donor cancels their own pending donation claim.
     */
    @Transactional
    public DonationDetailDto cancelClaim(UUID donationId, UUID donorUserId, String clientIp, String userAgent) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found with id: " + donationId));

        authorizationService.verifyCanCancelClaim(donorUserId, donation);

        donation.cancel(Instant.now());
        Donation saved = donationRepository.save(donation);

        auditService.logAuthEvent(
                "DONATION_CLAIM_CANCELLED",
                donorUserId,
                clientIp,
                userAgent,
                "{\"donationId\":\"" + saved.getId() + "\"}"
        );

        return buildDetailDto(saved);
    }

    /**
     * Administrator corrects a donation record with an audited justification.
     */
    @Transactional
    public DonationDetailDto adminCorrection(UUID donationId, UUID adminUserId, AdminCorrectionRequest request, String clientIp, String userAgent) {
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User not found: " + adminUserId));

        if (!admin.getRoles().contains(UserRole.ROLE_ADMIN)) {
            throw new UnauthorizedSessionAccessException("Only administrators can perform donation corrections.");
        }

        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found with id: " + donationId));

        DonationVerificationStatus oldStatus = donation.getVerificationStatus();
        donation.adminCorrection(request.getTargetStatus(), request.getCorrectionReason(), adminUserId, Instant.now());
        Donation saved = donationRepository.save(donation);

        // If status changed away from VERIFIED, recompute lastDonationDate from remaining verified donations
        if (oldStatus == DonationVerificationStatus.VERIFIED && request.getTargetStatus() != DonationVerificationStatus.VERIFIED) {
            recalculateDonorLastDonationDate(saved.getDonorUserId());
        } else if (request.getTargetStatus() == DonationVerificationStatus.VERIFIED) {
            updateDonorLastDonationDate(saved.getDonorUserId(), saved.getDonationDate());
        }

        auditService.logAuthEvent(
                "DONATION_ADMIN_CORRECTION",
                adminUserId,
                clientIp,
                userAgent,
                "{\"donationId\":\"" + saved.getId() + "\",\"oldStatus\":\"" + oldStatus + "\",\"newStatus\":\"" + saved.getVerificationStatus() + "\"}"
        );

        return buildDetailDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<DonationDto> getMyDonations(UUID donorUserId, Pageable pageable) {
        return donationRepository.findByDonorUserIdOrderByDonationDateDesc(donorUserId, pageable)
                .map(DonationDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<DonationDto> getMyDonationList(UUID donorUserId) {
        return donationRepository.findByDonorUserIdOrderByDonationDateDesc(donorUserId).stream()
                .map(DonationDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<DonationDetailDto> getPendingDonations(Pageable pageable) {
        return donationRepository.findByVerificationStatusOrderByCreatedAtDesc(DonationVerificationStatus.PENDING_VERIFICATION, pageable)
                .map(this::buildDetailDto);
    }

    @Transactional(readOnly = true)
    public DonationDetailDto getDonationDetail(UUID donationId, UUID currentUserId) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found with id: " + donationId));

        authorizationService.verifyCanViewDonation(currentUserId, donation);
        return buildDetailDto(donation);
    }

    private void validateDonorAccount(UUID donorUserId) {
        User donor = userRepository.findById(donorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Donor user not found: " + donorUserId));

        if (donor.getStatus() != UserStatus.ACTIVE) {
            throw new AccountStatusException("Donor account is not active. Status: " + donor.getStatus());
        }

        if (!donorProfileRepository.existsByUserId(donorUserId)) {
            throw new ResourceNotFoundException("Donor profile does not exist for user: " + donorUserId);
        }
    }

    private void validateDonationDate(LocalDate donationDate) {
        if (donationDate == null) {
            throw new ValidationException("Donation date must not be null.");
        }
        LocalDate today = LocalDate.now();
        if (donationDate.isAfter(today)) {
            throw new ValidationException("Donation date cannot be in the future.");
        }
        if (donationDate.isBefore(today.minusDays(90))) {
            throw new ValidationException("Donation date cannot be older than 90 days.");
        }
    }

    private void validateBloodRequestDonationEligibility(UUID donorUserId, UUID bloodRequestId, LocalDate donationDate) {
        if (bloodRequestId == null) {
            throw new ValidationException("bloodRequestId is required for BLOOD_REQUEST donation claims.");
        }

        BloodRequest request = bloodRequestRepository.findById(bloodRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found: " + bloodRequestId));

        // Check for existing active donation for this request
        boolean alreadyExists = donationRepository.existsByDonorUserIdAndBloodRequestIdAndVerificationStatusIn(
                donorUserId, bloodRequestId, List.of(DonationVerificationStatus.PENDING_VERIFICATION, DonationVerificationStatus.VERIFIED));
        if (alreadyExists) {
            throw new DuplicateResourceException("A donation record already exists for this blood request.");
        }

        // Must have an ACCEPTED match
        Optional<DonorMatch> matchOpt = donorMatchRepository.findByBloodRequestIdAndDonorUserId(bloodRequestId, donorUserId);
        if (matchOpt.isEmpty()) {
            throw new ValidationException("No match found for this donor on the specified blood request.");
        }

        DonorMatch match = matchOpt.get();
        if (match.getResponseStatus() != MatchStatus.ACCEPTED) {
            throw new ValidationException("Donor has not accepted a match for this blood request. Current status: " + match.getResponseStatus());
        }

        // Request must not be cancelled
        if (request.getStatus() == BloodRequestStatus.CANCELLED) {
            throw new ValidationException("Cannot claim a donation against a cancelled blood request.");
        }
    }

    private void validateEventDonationEligibility(UUID donorUserId, UUID donationEventId) {
        if (donationEventId == null) {
            throw new ValidationException("donationEventId is required for DONATION_EVENT donation claims.");
        }

        DonationEvent event = donationEventRepository.findById(donationEventId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation event not found: " + donationEventId));

        if (event.getStatus() == DonationEventStatus.DRAFT || event.getStatus() == DonationEventStatus.CANCELLED) {
            throw new ValidationException("Cannot claim a donation for an event that is " + event.getStatus());
        }

        boolean alreadyExists = donationRepository.existsByDonorUserIdAndDonationEventIdAndVerificationStatusIn(
                donorUserId, donationEventId, List.of(DonationVerificationStatus.PENDING_VERIFICATION, DonationVerificationStatus.VERIFIED));
        if (alreadyExists) {
            throw new DuplicateResourceException("A donation record already exists for this donation camp.");
        }

        Optional<DonationEventRegistration> regOpt = registrationRepository.findByEventIdAndDonorUserId(donationEventId, donorUserId);
        if (regOpt.isEmpty()) {
            throw new ValidationException("Donor was not registered for this donation event.");
        }

        DonationEventRegistration reg = regOpt.get();
        if (reg.getStatus() == DonationEventRegistrationStatus.CANCELLED ||
            reg.getStatus() == DonationEventRegistrationStatus.REJECTED ||
            reg.getStatus() == DonationEventRegistrationStatus.NO_SHOW) {
            throw new ValidationException("Donor registration for this event is " + reg.getStatus());
        }
    }

    private void updateDonorLastDonationDate(UUID donorUserId, LocalDate donationDate) {
        donorProfileRepository.findByUserId(donorUserId).ifPresent(profile -> {
            LocalDate currentLast = profile.getLastDonationDate();
            if (currentLast == null || donationDate.isAfter(currentLast)) {
                profile.setLastDonationDate(donationDate);
                profile.setUpdatedAt(Instant.now());
                donorProfileRepository.save(profile);
                log.info("Updated donor {} lastDonationDate to {}", donorUserId, donationDate);
            }
        });
    }

    private void recalculateDonorLastDonationDate(UUID donorUserId) {
        donorProfileRepository.findByUserId(donorUserId).ifPresent(profile -> {
            Optional<LocalDate> latestDate = donationRepository.findLatestVerifiedDonationDate(donorUserId);
            profile.setLastDonationDate(latestDate.orElse(null));
            profile.setUpdatedAt(Instant.now());
            donorProfileRepository.save(profile);
            log.info("Recalculated donor {} lastDonationDate to {}", donorUserId, profile.getLastDonationDate());
        });
    }

    private void markRegistrationCompleted(UUID eventId, UUID donorUserId, Instant completedAt) {
        registrationRepository.findByEventIdAndDonorUserId(eventId, donorUserId).ifPresent(reg -> {
            reg.setStatus(DonationEventRegistrationStatus.COMPLETED);
            reg.setCompletedAt(completedAt);
            reg.setUpdatedAt(completedAt);
            registrationRepository.save(reg);
        });
    }

    private DonationDetailDto buildDetailDto(Donation d) {
        String donorName = userRepository.findById(d.getDonorUserId())
                .map(User::getFullName)
                .orElse("Anonymous Donor");

        String refTitle = "Blood Donation";
        String refLocation = "NETRA Centre";

        if (d.getSourceType() == DonationSourceType.BLOOD_REQUEST && d.getBloodRequestId() != null) {
            BloodRequest req = bloodRequestRepository.findById(d.getBloodRequestId()).orElse(null);
            if (req != null) {
                refTitle = "Request for " + req.getBloodGroup().name() + " (" + req.getHospitalName() + ")";
                refLocation = req.getHospitalName() + ", " + req.getCity();
            }
        } else if (d.getSourceType() == DonationSourceType.DONATION_EVENT && d.getDonationEventId() != null) {
            DonationEvent evt = donationEventRepository.findById(d.getDonationEventId()).orElse(null);
            if (evt != null) {
                refTitle = evt.getTitle();
                refLocation = evt.getVenueName() + ", " + evt.getCity();
            }
        }

        return DonationDetailDto.fromEntity(d, donorName, refTitle, refLocation);
    }
}
